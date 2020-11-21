package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.action
import io.github.sds100.keymapper.constraint
import io.github.sds100.keymapper.data.model.ActionModel
import io.github.sds100.keymapper.data.model.ConstraintModel
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.data.viewmodel.FingerprintGestureViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.databinding.ListItemFingerprintGestureBinding
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

        mViewModel.models.observe(viewLifecycleOwner, { models ->
            binding.state = models

            if (models !is Data) return@observe

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

                        onEnabledSwitchChangeListener { _, isChecked ->
                            mViewModel.setEnabled(it.id, isChecked)
                        }

                        onAddConstraintClick { _ ->
                            val direction = HomeFragmentDirections.actionHomeFragmentToChooseConstraint(
                                FingerprintGestureUtils.ADD_CONSTRAINT_REQUEST_KEYS[it.id]!!)

                            findNavController().navigate(direction)
                        }

                        onBind { _, view, _ ->
                            (view.dataBinding as ListItemFingerprintGestureBinding).apply {
                                epoxyRecyclerViewActions.bindActions(it.id, it.actionModels)
                                epoxyRecyclerViewConstraints.bindConstraints(it.id, it.constraintModels)
                            }
                        }
                    }
                }
            }
        })

        viewLifecycleScope.launchWhenStarted {
            mViewModel.buildModels.collect { gestureMaps ->
                val models = gestureMaps.map {
                    FingerprintGestureMapListItemModel(
                        id = it.key,
                        header = str(FingerprintGestureUtils.HEADERS[it.key]!!),

                        actionModels = it.value.actionList.map { action ->
                            action.buildModel(requireContext(), mViewModel.getDeviceInfoList())
                        },

                        constraintModels = it.value.constraintList.map { constraint ->
                            constraint.buildModel(requireContext())
                        },

                        isEnabled = it.value.isEnabled
                    )
                }

                mViewModel.setModels(models)
            }
        }

        viewLifecycleScope.launchWhenStarted {
            mViewModel.editOptions.collect {
                val requestKey = FingerprintGestureUtils.OPTIONS_REQUEST_KEYS[it.gestureId]!!

                val direction =
                    HomeFragmentDirections.actionHomeFragmentToFingerprintGestureMapBehaviorFragment(it, requestKey)
                findNavController().navigate(direction)
            }
        }
    }

    private fun EpoxyRecyclerView.bindActions(gestureId: String, models: List<ActionModel>) = withModels {
        models.forEach {
            action {
                id(it.id)
                model(it)

                actionCount(models.size)

                onRemoveClick { _ ->
                    mViewModel.removeAction(gestureId, it.id)
                }
            }
        }
    }

    private fun EpoxyRecyclerView.bindConstraints(gestureId: String, models: List<ConstraintModel>) = withModels {
        models.forEach {
            constraint {
                id(it.id)
                model(it)

                onRemoveClick { _ ->
                    mViewModel.removeConstraint(gestureId, it.id)
                }

                val tintType = when {
                    it.hasError -> TintType.ERROR
                    it.iconTintOnSurface -> TintType.ON_SURFACE
                    else -> TintType.NONE
                }

                tintType(tintType)

                onFixClick { _ ->
                    if (it.failure is RecoverableFailure) {
                        mRecoverFailureDelegate.recover(requireActivity(), it.failure)
                    }
                }
            }
        }
    }
}