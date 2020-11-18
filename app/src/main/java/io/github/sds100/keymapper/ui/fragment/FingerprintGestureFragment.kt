package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.data.viewmodel.FingerprintGestureViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.fingerprintGesture
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.RecoverFailureDelegate
import io.github.sds100.keymapper.util.result.RecoverableFailure
import kotlinx.coroutines.flow.collect

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

    override fun onResume() {
        super.onResume()

        mViewModel.rebuildModels()
    }

    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        binding.caption = str(R.string.caption_fingerprint_gesture)

        viewLifecycleScope.launchWhenStarted {
            mViewModel.models.collect { models ->
                binding.state = models

                if (models !is Data) return@collect

                binding.epoxyRecyclerView.withModels {
                    models.data.forEach {
                        fingerprintGesture {
                            id(it.id)
                            model(it)

                            onChooseActionClick { _ ->
                                val direction = HomeFragmentDirections.actionHomeFragmentToChooseActionFragment(
                                    FingerprintGestureUtils.CHOOSE_ACTION_REQUEST_KEYS[it.id]!!)

                                findNavController().navigate(direction)
                            }

                            onRemoveActionClick { _ ->
                                mViewModel.removeAction(it.id)
                            }

                            fixAction { _ ->
                                if (it.actionModel?.failure is RecoverableFailure) {
                                    mRecoverFailureDelegate.recover(requireActivity(), it.actionModel.failure)
                                }
                            }

                            onEnabledSwitchChangeListener { _, isChecked ->
                                mViewModel.setEnabled(it.id, isChecked)
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleScope.launchWhenStarted {
            mViewModel.buildModels.collect { gestureMaps ->
                val models = gestureMaps.map {
                    FingerprintGestureMapListItemModel(
                        id = it.key,
                        header = str(FingerprintGestureUtils.HEADERS[it.key]!!),
                        actionModel = it.value.action?.buildModel(requireContext()),
                        isEnabled = it.value.isEnabled
                    )
                }

                mViewModel.setModels(models)
            }
        }
    }
}