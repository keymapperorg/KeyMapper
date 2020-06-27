package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.viewmodel.ActionOptionsViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentActionOptionsBinding
import io.github.sds100.keymapper.slider
import io.github.sds100.keymapper.util.EventObserver
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.int
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 27/06/2020.
 */
class ActionOptionsFragment : BottomSheetDialogFragment() {

    companion object {
        const val REQUEST_KEY = "request_choose_action_options"
        const val EXTRA_ACTION_OPTIONS = "extra_action_options"
    }

    private val mViewModel: ActionOptionsViewModel by viewModels {
        InjectorUtils.provideActionOptionsViewModel()
    }

    private val mController by lazy { Controller() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentActionOptionsBinding.inflate(layoutInflater).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            mViewModel.setInitialOptions(navArgs<ActionOptionsFragmentArgs>().value.StringNavArgChooseActionOptionsModel)

            epoxyRecyclerView.adapter = mController.adapter

            mViewModel.checkBoxModels.observe(viewLifecycleOwner) { models ->
                mController.checkBoxModels = models
            }

            mViewModel.sliderModels.observe(viewLifecycleOwner) { models ->
                mController.sliderModels = models.map {
                    SliderListItemModel(
                        id = it.extraId,
                        label = Extra.EXTRA_LABELS[it.extraId]!!,

                        sliderModel = SliderModel(
                            value = it.value,
                            isDefaultStepEnabled = true,
                            min = int(Extra.EXTRA_MIN_VALUES[it.extraId]!!),
                            max = int(Extra.EXTRA_MAX_VALUES[it.extraId]!!),
                            stepSize = int(Extra.EXTRA_STEP_SIZE_VALUES[it.extraId]!!))
                    )
                }
            }

            mViewModel.onSaveEvent.observe(viewLifecycleOwner, EventObserver {
                setFragmentResult(REQUEST_KEY, bundleOf(EXTRA_ACTION_OPTIONS to it))
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

        var checkBoxModels: List<CheckBoxOption> = listOf()
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
                    id(it.flagId)
                    primaryText(str(Action.ACTION_FLAG_LABEL_MAP[it.flagId]!!))
                    isSelected(it.isChecked)

                    onClick { _ ->
                        mViewModel.toggleFlag(it.flagId)
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
                        if (value < it.sliderModel.min) {
                            mViewModel.setOptionValue(it.id, ConfigKeymapViewModel.EXTRA_USE_DEFAULT)
                        } else {
                            mViewModel.setOptionValue(it.id, value.toInt())
                        }
                    }
                }
            }
        }
    }
}