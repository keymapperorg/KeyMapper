package io.github.sds100.keymapper.data.viewmodel

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.model.SliderModel
import io.github.sds100.keymapper.data.model.options.BehaviorOption
import io.github.sds100.keymapper.data.model.options.BehaviorOption.Companion.nullIfDefault
import io.github.sds100.keymapper.data.model.options.TriggerOptions

/**
 * Created by sds100 on 29/11/20.
 */
class TriggerOptionsViewModel : BaseOptionsViewModel<TriggerOptions>() {
    override fun createSliderListItemModel(option: BehaviorOption<Int>) = when (option.id) {

        TriggerOptions.ID_VIBRATE_DURATION -> SliderListItemModel(
            id = option.id,
            label = R.string.extra_label_vibration_duration,
            sliderModel = SliderModel(
                value = option.value.nullIfDefault,
                isDefaultStepEnabled = true,
                min = R.integer.vibrate_duration_min,
                maxSlider = R.integer.vibrate_duration_max,
                stepSize = R.integer.vibrate_duration_step_size
            )
        )


        TriggerOptions.ID_LONG_PRESS_DELAY -> SliderListItemModel(
            id = option.id,
            label = R.string.extra_label_long_press_delay_timeout,
            sliderModel = SliderModel(
                value = option.value.nullIfDefault,
                isDefaultStepEnabled = true,
                min = R.integer.long_press_delay_min,
                maxSlider = R.integer.long_press_delay_max,
                stepSize = R.integer.long_press_delay_step_size
            )
        )

        TriggerOptions.ID_DOUBLE_PRESS_DELAY -> SliderListItemModel(
            id = option.id,
            label = R.string.extra_label_double_press_delay_timeout,
            sliderModel = SliderModel(
                value = option.value.nullIfDefault,
                isDefaultStepEnabled = true,
                min = R.integer.double_press_delay_min,
                maxSlider = R.integer.double_press_delay_max,
                stepSize = R.integer.double_press_delay_step_size
            )
        )

        TriggerOptions.ID_SEQUENCE_TRIGGER_TIMEOUT -> SliderListItemModel(
            id = option.id,
            label = R.string.extra_label_sequence_trigger_timeout,
            sliderModel = SliderModel(
                value = option.value.nullIfDefault,
                isDefaultStepEnabled = true,
                min = R.integer.sequence_trigger_timeout_min,
                maxSlider = R.integer.sequence_trigger_timeout_max,
                stepSize = R.integer.sequence_trigger_timeout_step_size
            )
        )

        else -> throw Exception("Don't know how to create a SliderListItemModel for this option $option.id")
    }

    override fun createCheckboxListItemModel(option: BehaviorOption<Boolean>) = when (option.id) {
        TriggerOptions.ID_VIBRATE -> CheckBoxListItemModel(
            id = option.id,
            label = R.string.flag_vibrate,
            isChecked = option.value
        )

        TriggerOptions.ID_SCREEN_OFF_TRIGGER -> CheckBoxListItemModel(
            id = option.id,
            label = R.string.flag_detect_triggers_screen_off,
            isChecked = option.value
        )

        TriggerOptions.ID_LONG_PRESS_DOUBLE_VIBRATION -> CheckBoxListItemModel(
            id = option.id,
            label = R.string.flag_long_press_double_vibration,
            isChecked = option.value
        )

        else -> throw Exception("Don't know how to create a CheckboxListItemModel for this option $option.id")
    }
}