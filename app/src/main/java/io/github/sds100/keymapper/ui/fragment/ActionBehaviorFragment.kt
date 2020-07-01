package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.data.model.BehaviorOption
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.viewmodel.ActionBehaviorViewModel
import io.github.sds100.keymapper.databinding.FragmentActionBehaviorBinding
import io.github.sds100.keymapper.slider
import io.github.sds100.keymapper.util.EventObserver
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.int
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 27/06/2020.
 */
class ActionBehaviorFragment : BottomSheetDialogFragment() {

    companion object {
        const val REQUEST_KEY = "request_choose_action_options"
        const val EXTRA_ACTION_BEHAVIOR = "extra_action_behavior"
    }

    private val mViewModel: ActionBehaviorViewModel by viewModels {
        InjectorUtils.provideActionBehaviorViewModel()
    }

    private val mController by lazy { Controller() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentActionBehaviorBinding.inflate(layoutInflater).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            mViewModel.setBehavior(navArgs<ActionBehaviorFragmentArgs>().value.StringNavArgActionBehavior)

            epoxyRecyclerView.adapter = mController.adapter

            mViewModel.checkBoxModels.observe(viewLifecycleOwner) {
                mController.checkBoxModels = it
            }

            mViewModel.sliderModels.observe(viewLifecycleOwner) {
                mController.sliderModels = it
            }

            mViewModel.onSaveEvent.observe(viewLifecycleOwner, EventObserver {
                setFragmentResult(REQUEST_KEY, bundleOf(EXTRA_ACTION_BEHAVIOR to it))
                findNavController().navigateUp()
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
                }
            }
        }
    }
}