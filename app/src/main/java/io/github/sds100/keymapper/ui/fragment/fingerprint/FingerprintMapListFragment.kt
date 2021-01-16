package io.github.sds100.keymapper.ui.fragment.fingerprint

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.switchmaterial.SwitchMaterial
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.FingerprintMap
import io.github.sds100.keymapper.data.model.FingerprintMapListItemModel
import io.github.sds100.keymapper.data.viewmodel.BackupRestoreViewModel
import io.github.sds100.keymapper.data.viewmodel.FingerprintMapListViewModel
import io.github.sds100.keymapper.databinding.FragmentFingerprintMapListBinding
import io.github.sds100.keymapper.ui.callback.ErrorClickCallback
import io.github.sds100.keymapper.ui.fragment.RecyclerViewFragment
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.IModelState
import io.github.sds100.keymapper.util.delegate.RecoverFailureDelegate
import io.github.sds100.keymapper.util.result.Failure
import splitties.alertdialog.appcompat.*

/**
 * Created by sds100 on 11/12/2020.
 */
class FingerprintMapListFragment
    : RecyclerViewFragment<List<FingerprintMapListItemModel>, FragmentFingerprintMapListBinding>() {

    private val viewModel: FingerprintMapListViewModel by activityViewModels {
        InjectorUtils.provideFingerprintMapListViewModel(requireContext())
    }

    private val backupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            backupRestoreViewModel
                .backupFingerprintMaps(requireContext().contentResolver.openOutputStream(it))
        }

    private val backupRestoreViewModel: BackupRestoreViewModel by activityViewModels {
        InjectorUtils.provideBackupRestoreViewModel(requireContext())
    }

    override val modelState: IModelState<List<FingerprintMapListItemModel>>
        get() = viewModel

    private lateinit var recoverFailureDelegate: RecoverFailureDelegate

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        recoverFailureDelegate = RecoverFailureDelegate(
            "FingerprintGestureFragment",
            requireActivity().activityResultRegistry,
            viewLifecycleOwner) {

            viewModel.rebuildModels()
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun populateList(
        binding: FragmentFingerprintMapListBinding,
        model: List<FingerprintMapListItemModel>?
    ) {
        binding.epoxyRecyclerView.withModels {
            model?.forEach {
                fingerprintMap {
                    id(it.id)
                    model(it)

                    onEnabledSwitchClick { view ->
                        viewModel.setEnabled(it.id, (view as SwitchMaterial).isChecked)
                    }

                    onErrorClick(object : ErrorClickCallback {
                        override fun onErrorClick(failure: Failure) {
                            viewModel.fixError(failure)
                        }
                    })

                    onClick { _ ->
                        val direction = NavAppDirections.actionToConfigFingerprintMap(it.id)
                        findNavController().navigate(direction)
                    }
                }
            }
        }
    }

    override fun subscribeUi(binding: FragmentFingerprintMapListBinding) {
        binding.viewModel = viewModel

        viewModel.eventStream.observe(viewLifecycleOwner,
            {
                when (it) {
                    is BuildFingerprintMapModels -> {
                        viewLifecycleScope.launchWhenResumed {
                            viewModel.setModels(buildModels(it.maps))
                        }
                    }

                    is RequestFingerprintMapReset -> {
                        requireContext().alertDialog {
                            messageResource = R.string.dialog_title_are_you_sure

                            positiveButton(R.string.pos_yes) {
                                viewModel.reset()
                            }

                            cancelButton()

                            show()
                        }
                    }

                    is BackupFingerprintMaps -> backupLauncher.launch(BackupUtils.createFileName())
                }
            })

        viewModel.rebuildModels()
    }

    private suspend fun buildModels(maps: Map<String, FingerprintMap>) =
        maps.map {
            FingerprintMapListItemModel(
                id = it.key,
                header = str(FingerprintMapUtils.HEADERS[it.key]!!),

                actionModels = it.value.actionList.map { action ->
                    action.buildChipModel(requireContext(), viewModel.getDeviceInfoList())
                },

                constraintModels = it.value.constraintList.map { constraint ->
                    constraint.buildModel(requireContext())
                },

                constraintMode = it.value.constraintMode,

                isEnabled = it.value.isEnabled,

                optionsDescription = it.value.buildOptionsDescription(requireContext())
            )
        }

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFingerprintMapListBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
}