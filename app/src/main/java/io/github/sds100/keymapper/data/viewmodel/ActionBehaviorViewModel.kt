package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.ActionBehavior
import io.github.sds100.keymapper.data.model.BehaviorOption.Companion.nullIfDefault
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.model.SliderModel
import io.github.sds100.keymapper.util.Event

/**
 * Created by sds100 on 27/06/20.
 */
class ActionBehaviorViewModel : ViewModel() {

    companion object {
        private const val STATE_KEY = "state_choose_action_options"
    }

    val behavior: MediatorLiveData<ActionBehavior> = MediatorLiveData()

    val sliderModels = behavior.map {
        sequence {

            if (it.multiplier.isAllowed) {
                yield(SliderListItemModel(
                    id = it.multiplier.id,
                    label = R.string.extra_label_action_multiplier,

                    sliderModel = SliderModel(
                        value = it.multiplier.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.action_multiplier_min,
                        maxSlider = R.integer.action_multiplier_max,
                        stepSize = R.integer.action_multiplier_step_size
                    )
                ))
            }

            if (it.repeatRate.isAllowed) {
                yield(SliderListItemModel(
                    id = it.repeatRate.id,
                    label = R.string.extra_label_repeat_rate,
                    sliderModel = SliderModel(
                        value = it.repeatRate.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.repeat_rate_min,
                        max = R.integer.repeat_rate_max,
                        stepSize = R.integer.repeat_rate_step_size
                    )
                ))
            }

            if (it.repeatDelay.isAllowed) {
                yield(SliderListItemModel(
                    id = it.repeatDelay.id,
                    label = R.string.extra_label_repeat_delay,
                    sliderModel = SliderModel(
                        value = it.repeatDelay.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.repeat_delay_min,
                        max = R.integer.repeat_delay_max,
                        stepSize = R.integer.repeat_delay_step_size
                    )
                ))
            }
        }.toList()
    }

    val checkBoxModels = behavior.map {
        sequence {
            if (it.showPerformingActionToast.isAllowed) {
                yield(CheckBoxListItemModel(
                    id = it.showPerformingActionToast.id,
                    label = R.string.flag_performing_action_toast,
                    isChecked = it.showPerformingActionToast.value
                ))
            }

            if (it.showVolumeUi.isAllowed) {
                yield(CheckBoxListItemModel(
                    id = it.showVolumeUi.id,
                    label = R.string.flag_show_volume_dialog,
                    isChecked = it.showVolumeUi.value
                ))
            }

            if (it.repeat.isAllowed) {
                yield(CheckBoxListItemModel(
                    id = it.repeat.id,
                    label = R.string.flag_repeat_actions,
                    isChecked = it.repeat.value
                ))
            }

            if (it.holdDown.isAllowed) {
                yield(CheckBoxListItemModel(
                    id = it.holdDown.id,
                    label = R.string.flag_hold_down,
                    isChecked = it.holdDown.value
                ))
            }
        }.toList()
    }

    val stopRepeatWhenTriggerPressedAgain = MediatorLiveData<Boolean>().apply {
        addSource(behavior) {
            val newValue = it.stopRepeatingWhenTriggerPressedAgain.value

            if (newValue != value) {
                value = newValue
            }
        }
    }

    val stopRepeatWhenTriggerReleased = MediatorLiveData<Boolean>().apply {
        addSource(behavior) {
            val newValue = it.stopRepeatingWhenTriggerReleased.value

            if (newValue != value) {
                value = newValue
            }
        }
    }

    val onSaveEvent: MutableLiveData<Event<ActionBehavior>> = MutableLiveData()

    init {
        behavior.addSource(stopRepeatWhenTriggerReleased) {
            if (it != behavior.value?.stopRepeatingWhenTriggerReleased?.value) {
                setValue(ActionBehavior.ID_STOP_REPEATING_TRIGGER_RELEASED, it)
            }
        }

        behavior.addSource(stopRepeatWhenTriggerPressedAgain) {
            if (it != behavior.value?.stopRepeatingWhenTriggerPressedAgain?.value) {
                setValue(ActionBehavior.ID_STOP_REPEATING_TRIGGER_PRESSED_AGAIN, it)
            }
        }
    }

    fun setValue(id: String, newValue: Int) {
        behavior.value = behavior.value?.setValue(id, newValue)
    }

    fun setValue(id: String, newValue: Boolean) {
        behavior.value = behavior.value?.setValue(id, newValue)
    }

    fun setBehavior(actionBehavior: ActionBehavior) {
        behavior.value = actionBehavior
    }

    fun save() {
        onSaveEvent.value = Event(behavior.value!!)
    }

    fun saveState(outState: Bundle) {
        outState.putSerializable(STATE_KEY, behavior.value)
    }

    fun restoreState(state: Bundle) {
        val behavior = state.getSerializable(STATE_KEY) as ActionBehavior
        setBehavior(behavior)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ActionBehaviorViewModel() as T
        }
    }
}