package io.github.sds100.keymapper.data.model

import io.github.sds100.keymapper.data.model.BehaviorOption.Companion.applyBehaviorOption
import io.github.sds100.keymapper.util.ActionUtils
import io.github.sds100.keymapper.util.delegate.KeymapDetectionDelegate
import io.github.sds100.keymapper.util.result.onSuccess
import io.github.sds100.keymapper.util.result.valueOrNull
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag
import java.io.Serializable

/**
 * Created by sds100 on 27/06/20.
 */
class BehaviorOption<T>(val id: String, var value: T, var isAllowed: Boolean) {
    companion object {
        const val DEFAULT = -1

        fun List<Extra>.applyBehaviorOption(behaviorOption: BehaviorOption<Int>, @ExtraId extraId: String) =
            toMutableList().apply {
                removeAll { it.id == extraId }

                if (behaviorOption.isAllowed) {
                    if (behaviorOption.value != DEFAULT) {
                        add(Extra(extraId, behaviorOption.value.toString()))
                    }
                }
            }

        fun Int.applyBehaviorOption(behaviorOption: BehaviorOption<Boolean>, flagId: Int): Int {
            return if (behaviorOption.isAllowed) {
                if (behaviorOption.value) {
                    withFlag(flagId)
                } else {
                    minusFlag(flagId)
                }
            } else {
                minusFlag(flagId)
            }
        }

        val Int.nullIfDefault: Int?
            get() = if (this == DEFAULT) {
                null
            } else {
                this
            }
    }
}

class ActionBehavior(action: Action, @Trigger.Mode triggerMode: Int, triggerKeys: List<Trigger.Key>) : Serializable {
    companion object {
        const val ID_REPEAT_DELAY = "repeat_delay"
        const val ID_REPEAT = "repeat"
        const val ID_SHOW_VOLUME_UI = "show_volume_ui"
        const val ID_SHOW_PERFORMING_ACTION_TOAST = "show_performing_action_toast"
        const val ID_STOP_REPEATING_TRIGGER_RELEASED = "stop_repeating_trigger_released"
        const val ID_STOP_REPEATING_TRIGGER_PRESSED_AGAIN = "stop_repeating_trigger_pressed_again"
        const val ID_HOLD_DOWN_DELAY = "hold_down_delay"
    }

    val actionId = action.uniqueId

    val repeat = BehaviorOption(
        id = ID_REPEAT,
        value = action.flags.hasFlag(Action.ACTION_FLAG_REPEAT),
        isAllowed = KeymapDetectionDelegate.performActionOnDown(triggerKeys, triggerMode)
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

    val stopRepeatingWhenTriggerReleased: BehaviorOption<Boolean>

    val stopRepeatingWhenTriggerPressedAgain: BehaviorOption<Boolean>

    val repeatDelay: BehaviorOption<Int>

    val holdDownDelay: BehaviorOption<Int>

    init {
        val repeatDelayValue = action.getExtraData(Action.EXTRA_REPEAT_DELAY).valueOrNull()?.toInt()

        repeatDelay = BehaviorOption(
            id = ID_REPEAT_DELAY,
            value = repeatDelayValue ?: BehaviorOption.DEFAULT,
            isAllowed = repeat.value
        )

        val holdDownDelayValue = action.getExtraData(Action.EXTRA_HOLD_DOWN_DELAY).valueOrNull()?.toInt()

        holdDownDelay = BehaviorOption(
            id = ID_HOLD_DOWN_DELAY,
            value = holdDownDelayValue ?: BehaviorOption.DEFAULT,
            isAllowed = repeat.value
        )

        var stopRepeatingTriggerPressedAgain = false

        action.getExtraData(Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR).onSuccess {
            if (it.toInt() == Action.STOP_REPEAT_BEHAVIOUR_TRIGGER_AGAIN) {
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
    }

    fun applyToAction(action: Action): Action {
        val newFlags = action.flags
            .applyBehaviorOption(repeat, Action.ACTION_FLAG_REPEAT)
            .applyBehaviorOption(showVolumeUi, Action.ACTION_FLAG_SHOW_VOLUME_UI)
            .applyBehaviorOption(showPerformingActionToast, Action.ACTION_FLAG_SHOW_PERFORMING_ACTION_TOAST)

        val newExtras = action.extras
            .applyBehaviorOption(repeatDelay, Action.EXTRA_REPEAT_DELAY)
            .applyBehaviorOption(holdDownDelay, Action.EXTRA_HOLD_DOWN_DELAY)

        newExtras.removeAll { it.id == Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR }
        if (stopRepeatingWhenTriggerPressedAgain.value) {
            newExtras.add(
                Extra(Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR, Action.STOP_REPEAT_BEHAVIOUR_TRIGGER_AGAIN.toString()))
        }

        return action.clone(flags = newFlags, extras = newExtras)
    }

    fun setValue(id: String, value: Int): ActionBehavior {
        when (id) {
            ID_REPEAT_DELAY -> repeatDelay.value = value
            ID_HOLD_DOWN_DELAY -> holdDownDelay.value = value
        }

        return this
    }

    fun setValue(id: String, value: Boolean): ActionBehavior {
        when (id) {
            ID_REPEAT -> {
                repeatDelay.isAllowed = value
                holdDownDelay.isAllowed = value
                stopRepeatingWhenTriggerPressedAgain.isAllowed = value
                stopRepeatingWhenTriggerReleased.isAllowed = value

                repeat.value = value
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
        }

        return this
    }
}