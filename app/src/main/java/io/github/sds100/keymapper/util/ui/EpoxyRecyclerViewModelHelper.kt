package io.github.sds100.keymapper.ui.utils

import androidx.fragment.app.Fragment
import com.airbnb.epoxy.EpoxyController
import com.google.android.material.slider.Slider
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.databinding.ListItemCheckboxBinding
import io.github.sds100.keymapper.util.Defaultable
import io.github.sds100.keymapper.util.ui.*
import io.github.sds100.keymapper.util.viewLifecycleScope

/**
 * Created by sds100 on 20/03/2021.
 */

fun EpoxyController.configuredRadioButtonTriple(
    model: RadioButtonTripleListItem,
    onCheckedChange: (buttonId: String, isChecked: Boolean) -> Unit
) {
    radioButtonTriple {
        id(model.id)
        model(model)

        onCheckedChange { group, checkedId ->
            when (checkedId) {
                R.id.radioButtonLeft -> onCheckedChange(model.leftButtonId, true)
                R.id.radioButtonCenter -> onCheckedChange(model.centerButtonId, true)
                R.id.radioButtonRight -> onCheckedChange(model.rightButtonId, true)
            }
        }
    }
}

fun EpoxyController.configuredRadioButtonPair(
    model: RadioButtonPairListItem,
    onCheckedChange: (buttonId: String, isChecked: Boolean) -> Unit
) {
    radioButtonPair {
        id(model.id)
        model(model)

        onCheckedChange { group, checkedId ->
            when (checkedId) {
                R.id.radioButtonLeft -> onCheckedChange(model.leftButtonId, true)
                R.id.radioButtonRight -> onCheckedChange(model.rightButtonId, true)
            }
        }
    }
}

fun EpoxyController.configuredCheckBox(
    model: CheckBoxListItem,
    onCheckedChange: (checked: Boolean) -> Unit
) {
    checkbox {
        id(model.id)
        model(model)

        onBind { bindingModel, view, _ ->

            (view.dataBinding as ListItemCheckboxBinding).checkBox.apply {
                //this is very important so checkboxes in other recycler views aren't affected by the checked state changing.
                setOnCheckedChangeListener(null)

                isChecked = bindingModel.model().isChecked
                text = bindingModel.model().label

                setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange.invoke(isChecked)
                }
            }
        }
    }
}

fun EpoxyController.configuredSlider(
    fragment: Fragment,
    model: SliderListItem,
    onValueChanged: (newValue: Defaultable<Int>) -> Unit
) {
    fragment.apply {
        slider {
            id(model.id)
            label(model.label)
            model(model.sliderModel)

            /*
            Only change the model when the touch has been released because the otherwise jank happens
            because the list is trying to update dozens/100s of times super fast.
             */
            onSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {

                }

                override fun onStopTrackingTouch(slider: Slider) {
                    if (slider.isInTouchMode) {
                        val value = if (slider.value < model.sliderModel.min) {
                            Defaultable.Default
                        } else {
                            Defaultable.Custom(slider.value.toInt())
                        }

                        onValueChanged.invoke(value)
                    }
                }
            })

            onSliderChangeListener { slider, value, fromUser ->
                if (fromUser && !slider.isInTouchMode) {
                    if (value < model.sliderModel.min) {
                        onValueChanged.invoke(Defaultable.Default)
                    } else {
                        onValueChanged.invoke(Defaultable.Custom(value.toInt()))
                    }
                }
            }

            onSliderValueClickListener { _ ->
                viewLifecycleScope.launchWhenResumed {
                    val newValue = requireContext().editTextNumberAlertDialog(
                        viewLifecycleOwner,
                        hint = model.label,
                        min = model.sliderModel.min
                    ) ?: return@launchWhenResumed

                    onValueChanged.invoke(Defaultable.Custom(newValue))
                }
            }
        }
    }
}