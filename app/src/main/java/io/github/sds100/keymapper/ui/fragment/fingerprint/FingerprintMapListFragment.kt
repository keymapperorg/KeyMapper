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
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.data.model.FingerprintMap
import io.github.sds100.keymapper.data.viewmodel.BackupRestoreViewModel
import io.github.sds100.keymapper.data.viewmodel.FingerprintMapListViewModel
import io.github.sds100.keymapper.databinding.FragmentFingerprintMapListBinding
import io.github.sds100.keymapper.ui.callback.ErrorClickCallback
import io.github.sds100.keymapper.ui.fragment.RecyclerViewFragment
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.RecoverFailureDelegate
import io.github.sds100.keymapper.util.result.Failure
import splitties.alertdialog.appcompat.*

/**
 * Created by sds100 on 11/12/2020.
 */
class FingerprintMapListFragment : RecyclerViewFragment<FragmentFingerprintMapListBinding>() {

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

    override fun subscribeUi(binding: FragmentFingerprintMapListBinding) {
        viewModel.models.observe(viewLifecycleOwner, { models ->
            binding.viewModel = viewModel

            binding.state = models

            if (models !is Data) return@observe

            binding.epoxyRecyclerView.withModels {
                models.data.forEach {
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
        })

        viewModel.eventStream.observe(viewLifecycleOwner,
            {
                when (it) {
                    is BuildFingerprintMapModels -> {
                        viewLifecycleScope.launchWhenStarted {
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
            FingerprintGestureMapListItemModel(
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
        FragmentFingerprintMapListBinding.inflate(inflater, container)
}