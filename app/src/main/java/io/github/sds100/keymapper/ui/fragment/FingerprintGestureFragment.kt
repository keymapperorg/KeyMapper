package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.data.model.FingerprintGestureMap
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.data.viewmodel.FingerprintGestureViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.ui.callback.ErrorClickCallback
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.RecoverFailureDelegate
import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 22/02/2020.
 */
class FingerprintGestureFragment : DefaultRecyclerViewFragment() {

    private val mViewModel: FingerprintGestureViewModel by activityViewModels {
        InjectorUtils.provideFingerprintGestureViewModel(requireContext())
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

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        mViewModel.models.observe(viewLifecycleOwner, { models ->
            binding.state = models

            if (models !is Data) return@observe

            binding.epoxyRecyclerView.withModels {
                models.data.forEach {
                    fingerprintGesture {
                        id(it.id)
                        model(it)

                        onEnabledSwitchChangeListener { _, isChecked ->
                            mViewModel.setEnabled(it.id, isChecked)
                        }

                        onErrorClick(object : ErrorClickCallback {
                            override fun onErrorClick(failure: Failure) {
                                mViewModel.fixError(failure)
                            }
                        })

                        onClick { _ ->
                        }
                    }
                }
            }
        })

        mViewModel.eventStream.observe(viewLifecycleOwner,
            {
                when (it) {
                    is BuildFingerprintGestureModels -> {
                        viewLifecycleScope.launchWhenStarted {
                            mViewModel.setModels(buildModels(it.gestureMaps))
                        }
                    }
                }
            })

        mViewModel.rebuildModels()
    }

    private suspend fun buildModels(gestureMaps: Map<String, FingerprintGestureMap>) =
        gestureMaps.map {
            FingerprintGestureMapListItemModel(
                id = it.key,
                header = str(FingerprintGestureUtils.HEADERS[it.key]!!),

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
}