package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.model.options.BehaviorOption
import io.github.sds100.keymapper.data.viewmodel.FingerprintGestureMapOptionsViewModel
import io.github.sds100.keymapper.databinding.FragmentFingerprintGestureMapBehaviorBinding
import io.github.sds100.keymapper.slider
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.collect

/**
 * Created by sds100 on 27/06/2020.
 */
class FingerprintGestureMapOptionsFragment : BottomSheetDialogFragment() {

    companion object {
        const val EXTRA_FINGERPRINT_GESTURE_MAP_OPTIONS = "extra_fingerprint_gesture_map_behavior"
    }

    private val mViewModel: FingerprintGestureMapOptionsViewModel by viewModels {
        InjectorUtils.provideFingerprintGestureMapBehaviorViewModel()
    }

    private val mRequestKey by lazy { navArgs<FingerprintGestureMapOptionsFragmentArgs>().value.requestKey }

    private val mController by lazy { Controller() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentFingerprintGestureMapBehaviorBinding.inflate(layoutInflater).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            mViewModel.setOptions(navArgs<FingerprintGestureMapOptionsFragmentArgs>().value.options)

            epoxyRecyclerView.adapter = mController.adapter

            mViewModel.options.observe(viewLifecycleOwner, {
                isLoading = it is Loading
            })

            viewLifecycleScope.launchWhenStarted {
                mViewModel.onSave.collect {
                    setFragmentResult(mRequestKey, bundleOf(EXTRA_FINGERPRINT_GESTURE_MAP_OPTIONS to it))
                    findNavController().navigateUp()
                }
            }

            mViewModel.checkBoxModels.observe(viewLifecycleOwner, {
                mController.checkBoxModels = it
            })

            mViewModel.sliderModels.observe(viewLifecycleOwner, {
                mController.sliderModels = it
            })

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = requireDialog() as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mViewModel.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState ?: return

        mViewModel.restoreState(savedInstanceState)
    }

    private inner class Controller : EpoxyController() {

        var checkBoxModels: List<CheckBoxListItemModel> = listOf()
            set(value) {
                field = value
                requestModelBuild()
            }

        var sliderModels: List<SliderListItemModel> = listOf()
            set(value) {
                field = value
                requestModelBuild()
            }

        override fun buildModels() {
            checkBoxModels.forEach {
                checkbox {
                    id(it.id)
                    primaryText(str(it.label))
                    isSelected(it.isChecked)

                    onClick { view ->
                        mViewModel.setValue(it.id, (view as CheckBox).isChecked)
                    }
                }
            }

            sliderModels.forEach {
                slider {
                    id(it.id)
                    label(str(it.label))
                    model(it.sliderModel)

                    onSliderChangeListener { _, value, fromUser ->
                        if (!fromUser) return@onSliderChangeListener

                        //If the user has selected to use the default value
                        if (value < int(it.sliderModel.min)) {
                            mViewModel.setValue(it.id, BehaviorOption.DEFAULT)
                        } else {
                            mViewModel.setValue(it.id, value.toInt())
                        }
                    }

                    onSliderValueClickListener { _ ->
                        lifecycleScope.launchWhenStarted {
                            val num = requireActivity().editTextNumberAlertDialog(
                                hint = str(it.label),
                                min = int(it.sliderModel.min))

                            mViewModel.setValue(it.id, num)
                        }
                    }
                }
            }
        }
    }
}