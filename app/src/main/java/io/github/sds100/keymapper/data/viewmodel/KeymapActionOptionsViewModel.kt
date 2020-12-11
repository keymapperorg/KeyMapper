package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.model.SliderModel
import io.github.sds100.keymapper.data.model.options.BoolOption
import io.github.sds100.keymapper.data.model.options.IntOption
import io.github.sds100.keymapper.data.model.options.IntOption.Companion.nullIfDefault
import io.github.sds100.keymapper.data.model.options.KeymapActionOptions

/**
 * Created by sds100 on 27/06/20.
 */
class KeymapActionOptionsViewModel : BaseOptionsDialogViewModel<KeymapActionOptions>() {

    override val stateKey = "state_choose_action_options"

    val stopRepeatWhenTriggerPressedAgain = MediatorLiveData<Boolean>().apply {
        addSource(options) {
            val newValue = it.stopRepeatingWhenTriggerPressedAgain.value

            if (newValue != value) {
                value = newValue
            }
        }
    }

    val stopRepeatWhenTriggerReleased = MediatorLiveData<Boolean>().apply {
        addSource(options) {
            val newValue = it.stopRepeatingWhenTriggerReleased.value

            if (newValue != value) {
                value = newValue
            }
        }
    }

    val stopHoldDownWhenTriggerPressedAgain = MediatorLiveData<Boolean>().apply {
        addSource(options) {
            val newValue = it.stopHoldDownWhenTriggerPressedAgain.value

            if (newValue != value) {
                value = newValue
            }
        }
    }

    val stopHoldDownWhenTriggerReleased = MediatorLiveData<Boolean>().apply {
        addSource(options) {
            val newValue = it.stopHoldDownWhenTriggerReleased.value

            if (newValue != value) {
                value = newValue
            }
        }
    }

    init {
        options.addSource(stopRepeatWhenTriggerReleased) {
            if (it != options.value?.stopRepeatingWhenTriggerReleased?.value) {
                setValue(KeymapActionOptions.ID_STOP_REPEATING_TRIGGER_RELEASED, it)
            }
        }

        options.addSource(stopRepeatWhenTriggerPressedAgain) {
            if (it != options.value?.stopRepeatingWhenTriggerPressedAgain?.value) {
                setValue(KeymapActionOptions.ID_STOP_REPEATING_TRIGGER_PRESSED_AGAIN, it)
            }
        }

        options.addSource(stopHoldDownWhenTriggerReleased) {
            if (it != options.value?.stopHoldDownWhenTriggerReleased?.value) {
                setValue(KeymapActionOptions.ID_STOP_HOLD_DOWN_WHEN_TRIGGER_RELEASED, it)
            }
        }

        options.addSource(stopHoldDownWhenTriggerPressedAgain) {
            if (it != options.value?.stopHoldDownWhenTriggerPressedAgain?.value) {
                setValue(KeymapActionOptions.ID_STOP_HOLD_DOWN_WHEN_TRIGGER_PRESSED_AGAIN, it)
            }
        }
    }

    override fun createSliderListItemModel(option: IntOption): SliderListItemModel =
        when (option.id) {

            KeymapActionOptions.ID_MULTIPLIER -> {
                SliderListItemModel(
                    id = option.id,
                    label = R.string.extra_label_action_multiplier,

                    sliderModel = SliderModel(
                        value = option.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.action_multiplier_min,
                        maxSlider = R.integer.action_multiplier_max,
                        stepSize = R.integer.action_multiplier_step_size
                    )
                )
            }

            KeymapActionOptions.ID_REPEAT_RATE -> {
                SliderListItemModel(
                    id = option.id,
                    label = R.string.extra_label_repeat_rate,
                    sliderModel = SliderModel(
                        value = option.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.repeat_rate_min,
                        maxSlider = R.integer.repeat_rate_max,
                        stepSize = R.integer.repeat_rate_step_size
                    )
                )
            }

            KeymapActionOptions.ID_REPEAT_DELAY -> {
                SliderListItemModel(
                    id = option.id,
                    label = R.string.extra_label_repeat_delay,
                    sliderModel = SliderModel(
                        value = option.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.repeat_delay_min,
                        maxSlider = R.integer.repeat_delay_max,
                        stepSize = R.integer.repeat_delay_step_size
                    )
                )
            }

            KeymapActionOptions.ID_DELAY_BEFORE_NEXT_ACTION -> {
                SliderListItemModel(
                    id = option.id,
                    label = R.string.extra_label_delay_before_next_action,
                    sliderModel = SliderModel(
                        value = option.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.delay_before_next_action_min,
                        maxSlider = R.integer.delay_before_next_action_max,
                        stepSize = R.integer.delay_before_next_action_step_size
                    )
                )
            }

            KeymapActionOptions.ID_HOLD_DOWN_DURATION -> {
                SliderListItemModel(
                    id = option.id,
                    label = R.string.extra_label_hold_down_duration,
                    sliderModel = SliderModel(
                        value = option.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.hold_down_duration_min,
                        maxSlider = R.integer.hold_down_duration_max,
                        stepSize = R.integer.hold_down_duration_step_size
                    )
                )
            }
            else -> throw Exception("Don't know how to create a SliderListItemModel for this option $option.id")
        }

    override fun createCheckboxListItemModel(option: BoolOption): CheckBoxListItemModel =
        when (option.id) {
            KeymapActionOptions.ID_SHOW_PERFORMING_ACTION_TOAST -> {
                CheckBoxListItemModel(
                    id = option.id,
                    label = R.string.flag_performing_action_toast,
                    isChecked = option.value
                )
            }

            KeymapActionOptions.ID_SHOW_VOLUME_UI -> {
                CheckBoxListItemModel(
                    id = option.id,
                    label = R.string.flag_show_volume_dialog,
                    isChecked = option.value
                )
            }

            KeymapActionOptions.ID_REPEAT -> CheckBoxListItemModel(
                id = option.id,
                label = R.string.flag_repeat_actions,
                isChecked = option.value
            )

            KeymapActionOptions.ID_HOLD_DOWN -> CheckBoxListItemModel(
                id = option.id,
                label = R.string.flag_hold_down,
                isChecked = option.value
            )

            else -> throw Exception("Don't know how to create a SliderListItemModel for this option $option.id")
        }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return KeymapActionOptionsViewModel() as T
        }
    }
}