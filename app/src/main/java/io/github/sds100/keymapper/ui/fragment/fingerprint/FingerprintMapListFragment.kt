package io.github.sds100.keymapper.ui.fragment.fingerprint

import android.os.Bundle
import android.view.LayoutInflater
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

    private val mViewModel: FingerprintMapListViewModel by activityViewModels {
        InjectorUtils.provideFingerprintMapListViewModel(requireContext())
    }

    private val mBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            mBackupRestoreViewModel
                .backupFingerprintMaps(requireActivity().contentResolver.openOutputStream(it))
        }

    private val mBackupRestoreViewModel: BackupRestoreViewModel by activityViewModels {
        InjectorUtils.provideBackupRestoreViewModel(requireContext())
    }

    private lateinit var mRecoverFailureDelegate: RecoverFailureDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mRecoverFailureDelegate = RecoverFailureDelegate(
            "FingerprintGestureFragment",
            requireActivity().activityResultRegistry,
            this) {

            mViewModel.rebuildModels()
        }
    }

    override fun subscribeUi(binding: FragmentFingerprintMapListBinding) {
        mViewModel.models.observe(viewLifecycleOwner, { models ->
            binding.viewModel = mViewModel

            binding.state = models

            if (models !is Data) return@observe

            binding.epoxyRecyclerView.withModels {
                models.data.forEach {
                    fingerprintMap {
                        id(it.id)
                        model(it)

                        onEnabledSwitchClick { view ->
                            mViewModel.setEnabled(it.id, (view as SwitchMaterial).isChecked)
                        }

                        onErrorClick(object : ErrorClickCallback {
                            override fun onErrorClick(failure: Failure) {
                                mViewModel.fixError(failure)
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

        mViewModel.eventStream.observe(viewLifecycleOwner,
            {
                when (it) {
                    is BuildFingerprintMapModels -> {
                        viewLifecycleScope.launchWhenStarted {
                            mViewModel.setModels(buildModels(it.maps))
                        }
                    }

                    is RequestFingerprintMapReset -> {
                        requireActivity().alertDialog {
                            messageResource = R.string.dialog_title_are_you_sure

                            positiveButton(R.string.pos_yes) {
                                mViewModel.reset()
                            }

                            cancelButton()

                            show()
                        }
                    }

                    is BackupFingerprintMaps -> mBackupLauncher.launch(BackupUtils.createFileName())
                }
            })

        mViewModel.rebuildModels()
    }

    private suspend fun buildModels(maps: Map<String, FingerprintMap>) =
        maps.map {
            FingerprintGestureMapListItemModel(
                id = it.key,
                header = str(FingerprintMapUtils.HEADERS[it.key]!!),

                actionModels = it.value.actionList.map { action ->
                    action.buildChipModel(requireContext(), mViewModel.getDeviceInfoList())
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