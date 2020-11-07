package io.github.sds100.keymapper.data.model.behavior

import android.os.Build
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.behavior.BehaviorOption.Companion.applyBehaviorOption
import io.github.sds100.keymapper.data.model.getData
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.ActionUtils
import io.github.sds100.keymapper.util.delegate.KeymapDetectionDelegate
import io.github.sds100.keymapper.util.result.onSuccess
import io.github.sds100.keymapper.util.result.valueOrNull
import splitties.bitflags.hasFlag
import java.io.Serializable

class ActionBehavior(
    action: Action,
    actionCount: Int,
    @Trigger.Mode triggerMode: Int? = null,
    triggerKeys: List<Trigger.Key>? = null
) : Serializable {
    companion object {
        const val ID_REPEAT_RATE = "repeat_rate"
        const val ID_REPEAT = "repeat"
        const val ID_SHOW_VOLUME_UI = "show_volume_ui"
        const val ID_SHOW_PERFORMING_ACTION_TOAST = "show_performing_action_toast"
        const val ID_STOP_REPEATING_TRIGGER_RELEASED = "stop_repeating_trigger_released"
        const val ID_MULTIPLIER = "multiplier"
        const val ID_STOP_REPEATING_TRIGGER_PRESSED_AGAIN = "stop_repeating_trigger_pressed_again"
        const val ID_REPEAT_DELAY = "repeat_delay"
        const val ID_HOLD_DOWN = "hold_down"
        const val ID_STOP_HOLD_DOWN_WHEN_TRIGGER_RELEASED = "stop_hold_down_when_trigger_released"
        const val ID_STOP_HOLD_DOWN_WHEN_TRIGGER_PRESSED_AGAIN = "stop_hold_down_when_trigger_pressed_again"
        const val ID_DELAY_BEFORE_NEXT_ACTION = "delay_before_next_action"
    }

    val actionId = action.uniqueId

    val repeat = BehaviorOption(
        id = ID_REPEAT,
        value = action.flags.hasFlag(Action.ACTION_FLAG_REPEAT),
        isAllowed = if (triggerKeys != null && triggerMode != null) {
            KeymapDetectionDelegate.performActionOnDown(triggerKeys, triggerMode)
        } else {
            false
        }
    )

    val showVolumeUi = BehaviorOption(
        id = ID_SHOW_VOLUME_UI,
        value = action.flags.hasFlag(Action.ACTION_FLAG_SHOW_VOLUME_UI),
        isAllowed = ActionUtils.isVolumeAction(action.data)
    )

    val showPerformingActionToast = BehaviorOption(
        id = ID_SHOW_PERFORMING_ACTION_TOAST,
        value = action.flags.hasFlag(Action.ACTION_FLAG_SHOW_PERFORMING_ACTION_TOAST),
        isAllowed = true
    )

    val holdDown = BehaviorOption(
        id = ID_HOLD_DOWN,
        value = action.flags.hasFlag(Action.ACTION_FLAG_HOLD_DOWN),
        isAllowed = if (triggerKeys != null && triggerMode != null) {
            KeymapDetectionDelegate.performActionOnDown(triggerKeys, triggerMode)
                && (action.type == ActionType.KEY_EVENT
                || (action.type == ActionType.TAP_COORDINATE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                )
        } else {
            false
        }
    )

    val stopRepeatingWhenTriggerReleased: BehaviorOption<Boolean>
    val stopRepeatingWhenTriggerPressedAgain: BehaviorOption<Boolean>

    val stopHoldDownWhenTriggerReleased: BehaviorOption<Boolean>
    val stopHoldDownWhenTriggerPressedAgain: BehaviorOption<Boolean>

    val repeatRate: BehaviorOption<Int>

    val repeatDelay: BehaviorOption<Int>

    val delayBeforeNextAction: BehaviorOption<Int>

    val multiplier = BehaviorOption(
        id = ID_MULTIPLIER,
        value = action.extras.getData(Action.EXTRA_MULTIPLIER).valueOrNull()?.toInt() ?: BehaviorOption.DEFAULT,
        isAllowed = true
    )

    /*
     It is very important that any new options are only allowed with a valid combination of other options. Make sure
     the "isAllowed" property considers all the other options.
     */

    init {
        val repeatRateValue = action.extras.getData(Action.EXTRA_REPEAT_RATE).valueOrNull()?.toInt()

        repeatRate = BehaviorOption(
            id = ID_REPEAT_RATE,
            value = repeatRateValue ?: BehaviorOption.DEFAULT,
            isAllowed = repeat.value
        )

        val repeatDelayValue = action.extras.getData(Action.EXTRA_REPEAT_DELAY).valueOrNull()?.toInt()

        repeatDelay = BehaviorOption(
            id = ID_REPEAT_DELAY,
            value = repeatDelayValue ?: BehaviorOption.DEFAULT,
            isAllowed = repeat.value
        )

        var stopRepeatingTriggerPressedAgain = false

        action.extras.getData(Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR).onSuccess {
            if (it.toInt() == Action.STOP_REPEAT_BEHAVIOUR_TRIGGER_PRESSED_AGAIN) {
                stopRepeatingTriggerPressedAgain = true
            }
        }

        stopRepeatingWhenTriggerPressedAgain = BehaviorOption(
            id = ID_STOP_REPEATING_TRIGGER_PRESSED_AGAIN,
            value = stopRepeatingTriggerPressedAgain,
            isAllowed = repeat.value
        )

        stopRepeatingWhenTriggerReleased = BehaviorOption(
            id = ID_STOP_REPEATING_TRIGGER_RELEASED,
            value = !stopRepeatingTriggerPressedAgain,
            isAllowed = repeat.value
        )

        var stopHoldDownTriggerPressedAgain = false

        action.extras.getData(Action.EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR).onSuccess {
            if (it.toInt() == Action.STOP_HOLD_DOWN_BEHAVIOR_TRIGGER_PRESSED_AGAIN) {
                stopHoldDownTriggerPressedAgain = true
            }
        }

        stopHoldDownWhenTriggerPressedAgain = BehaviorOption(
            id = ID_STOP_HOLD_DOWN_WHEN_TRIGGER_PRESSED_AGAIN,
            value = stopHoldDownTriggerPressedAgain,
            isAllowed = holdDown.value && !repeat.value
        )

        stopHoldDownWhenTriggerReleased = BehaviorOption(
            id = ID_STOP_HOLD_DOWN_WHEN_TRIGGER_RELEASED,
            value = !stopHoldDownTriggerPressedAgain,
            isAllowed = holdDown.value
        )

        val delayBeforeNextActionValue =
            action.extras.getData(Action.EXTRA_DELAY_BEFORE_NEXT_ACTION).valueOrNull()?.toInt()

        delayBeforeNextAction = BehaviorOption(
            id = ID_DELAY_BEFORE_NEXT_ACTION,
            value = delayBeforeNextActionValue ?: BehaviorOption.DEFAULT,
            isAllowed = actionCount > 0
        )
    }

    fun applyToAction(action: Action): Action {
        val newFlags = action.flags
            .applyBehaviorOption(repeat, Action.ACTION_FLAG_REPEAT)
            .applyBehaviorOption(showVolumeUi, Action.ACTION_FLAG_SHOW_VOLUME_UI)
            .applyBehaviorOption(showPerformingActionToast, Action.ACTION_FLAG_SHOW_PERFORMING_ACTION_TOAST)
            .applyBehaviorOption(holdDown, Action.ACTION_FLAG_HOLD_DOWN)

        val newExtras = action.extras
            .applyBehaviorOption(repeatRate, Action.EXTRA_REPEAT_RATE)
            .applyBehaviorOption(repeatDelay, Action.EXTRA_REPEAT_DELAY)
            .applyBehaviorOption(multiplier, Action.EXTRA_MULTIPLIER)
            .applyBehaviorOption(delayBeforeNextAction, Action.EXTRA_DELAY_BEFORE_NEXT_ACTION)

        newExtras.removeAll {
            it.id in arrayOf(Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR, Action.EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR)
        }

        if (stopRepeatingWhenTriggerPressedAgain.value) {
            newExtras.add(
                Extra(
                    Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR,
                    Action.STOP_REPEAT_BEHAVIOUR_TRIGGER_PRESSED_AGAIN.toString()
                ))
        }

        if (stopHoldDownWhenTriggerPressedAgain.value) {
            newExtras.add(
                Extra(
                    Action.EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR,
                    Action.STOP_HOLD_DOWN_BEHAVIOR_TRIGGER_PRESSED_AGAIN.toString()
                ))
        }

        return action.clone(flags = newFlags, extras = newExtras)
    }

    fun setValue(id: String, value: Int): ActionBehavior {
        when (id) {
            ID_REPEAT_RATE -> repeatRate.value = value
            ID_REPEAT_DELAY -> repeatDelay.value = value
            ID_MULTIPLIER -> multiplier.value = value
            ID_DELAY_BEFORE_NEXT_ACTION -> delayBeforeNextAction.value = value
        }

        return this
    }

    fun setValue(id: String, value: Boolean): ActionBehavior {
        when (id) {
            ID_REPEAT -> {
                repeatRate.isAllowed = value
                repeatDelay.isAllowed = value
                stopRepeatingWhenTriggerPressedAgain.isAllowed = value
                stopRepeatingWhenTriggerReleased.isAllowed = value

                repeat.value = value

                stopHoldDownWhenTriggerPressedAgain.isAllowed = !value
                stopHoldDownWhenTriggerReleased.isAllowed = !value

                if (value && stopHoldDownWhenTriggerPressedAgain.value) {
                    setValue(ID_STOP_HOLD_DOWN_WHEN_TRIGGER_PRESSED_AGAIN, false)
                }
            }

            ID_SHOW_VOLUME_UI -> showVolumeUi.value = value
            ID_SHOW_PERFORMING_ACTION_TOAST -> showPerformingActionToast.value = value

            ID_STOP_REPEATING_TRIGGER_PRESSED_AGAIN -> {
                stopRepeatingWhenTriggerPressedAgain.value = value
                stopRepeatingWhenTriggerReleased.value = !stopRepeatingWhenTriggerPressedAgain.value
            }

            ID_STOP_REPEATING_TRIGGER_RELEASED -> {
                stopRepeatingWhenTriggerReleased.value = value
                stopRepeatingWhenTriggerPressedAgain.value = !stopRepeatingWhenTriggerReleased.value
            }

            ID_HOLD_DOWN -> {
                holdDown.value = value

                stopHoldDownWhenTriggerPressedAgain.isAllowed = holdDown.value && !repeat.value
                stopHoldDownWhenTriggerReleased.isAllowed = holdDown.value && !repeat.value
            }

            ID_STOP_HOLD_DOWN_WHEN_TRIGGER_RELEASED -> {
                stopHoldDownWhenTriggerReleased.value = value
                stopHoldDownWhenTriggerPressedAgain.value = !value
            }

            ID_STOP_HOLD_DOWN_WHEN_TRIGGER_PRESSED_AGAIN -> {
                stopHoldDownWhenTriggerPressedAgain.value = value
                stopHoldDownWhenTriggerReleased.value = !value
            }
        }

        return this
    }
}