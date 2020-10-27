package io.github.sds100.keymapper.data.model

import android.os.Build
import io.github.sds100.keymapper.data.model.BehaviorOption.Companion.applyBehaviorOption
import io.github.sds100.keymapper.data.model.Trigger.Companion.DOUBLE_PRESS
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_DOUBLE_PRESS_DELAY
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_LONG_PRESS_DELAY
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_SEQUENCE_TRIGGER_TIMEOUT
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_VIBRATION_DURATION
import io.github.sds100.keymapper.data.model.Trigger.Companion.LONG_PRESS
import io.github.sds100.keymapper.data.model.Trigger.Companion.PARALLEL
import io.github.sds100.keymapper.data.model.Trigger.Companion.SEQUENCE
import io.github.sds100.keymapper.data.model.Trigger.Companion.TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION
import io.github.sds100.keymapper.data.model.Trigger.Companion.TRIGGER_FLAG_SCREEN_OFF_TRIGGERS
import io.github.sds100.keymapper.data.model.Trigger.Companion.TRIGGER_FLAG_VIBRATE
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.ActionUtils
import io.github.sds100.keymapper.util.KeyEventUtils
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
class BehaviorOption<T>(val id: String, var value: T, var isAllowed: Boolean) : Serializable {
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

class TriggerBehavior(keys: List<Trigger.Key>, @Trigger.Mode mode: Int, flags: Int, extras: List<Extra>) {
    companion object {
        const val ID_LONG_PRESS_DELAY = "long_press_delay"
        const val ID_DOUBLE_PRESS_DELAY = "double_press_delay"
        const val ID_SEQUENCE_TRIGGER_TIMEOUT = "sequence_trigger_timeout"
        const val ID_VIBRATE_DURATION = "vibrate_duration"
        const val ID_VIBRATE = "vibrate"
        const val ID_LONG_PRESS_DOUBLE_VIBRATION = "long_press_double_vibration"
        const val ID_SCREEN_OFF_TRIGGER = "screen_off_trigger"
    }

    val vibrate = BehaviorOption(
        id = ID_VIBRATE,
        value = flags.hasFlag(TRIGGER_FLAG_VIBRATE),
        isAllowed = true
    )

    val longPressDoubleVibration = BehaviorOption(
        id = ID_LONG_PRESS_DOUBLE_VIBRATION,
        value = flags.hasFlag(TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION),
        isAllowed = (keys.size == 1 || (mode == PARALLEL))
            && keys.getOrNull(0)?.clickType == LONG_PRESS
    )

    val screenOffTrigger = BehaviorOption(
        id = ID_SCREEN_OFF_TRIGGER,
        value = flags.hasFlag(TRIGGER_FLAG_SCREEN_OFF_TRIGGERS),
        isAllowed = keys.isNotEmpty() && keys.all {
            KeyEventUtils.GET_EVENT_LABEL_TO_KEYCODE.containsValue(it.keyCode)
        }
    )

    val longPressDelay: BehaviorOption<Int>
    val doublePressDelay: BehaviorOption<Int>
    val vibrateDuration: BehaviorOption<Int>
    val sequenceTriggerTimeout: BehaviorOption<Int>

    init {

        val longPressDelayValue = extras.getData(EXTRA_LONG_PRESS_DELAY).valueOrNull()?.toInt()

        longPressDelay = BehaviorOption(
            id = ID_LONG_PRESS_DELAY,
            value = longPressDelayValue ?: BehaviorOption.DEFAULT,
            isAllowed = keys.any { it.clickType == LONG_PRESS }
        )

        val doublePressDelayValue = extras.getData(EXTRA_DOUBLE_PRESS_DELAY).valueOrNull()?.toInt()

        doublePressDelay = BehaviorOption(
            id = ID_DOUBLE_PRESS_DELAY,
            value = doublePressDelayValue ?: BehaviorOption.DEFAULT,
            isAllowed = keys.any { it.clickType == DOUBLE_PRESS }
        )

        val vibrateDurationValue = extras.getData(EXTRA_VIBRATION_DURATION).valueOrNull()?.toInt()

        vibrateDuration = BehaviorOption(
            id = ID_VIBRATE_DURATION,
            value = vibrateDurationValue ?: BehaviorOption.DEFAULT,
            isAllowed = vibrate.value || longPressDoubleVibration.value
        )

        val sequenceTriggerTimeoutValue =
            extras.getData(EXTRA_SEQUENCE_TRIGGER_TIMEOUT).valueOrNull()?.toInt()

        sequenceTriggerTimeout = BehaviorOption(
            id = ID_SEQUENCE_TRIGGER_TIMEOUT,
            value = sequenceTriggerTimeoutValue ?: BehaviorOption.DEFAULT,
            isAllowed = !keys.isNullOrEmpty() && keys.size > 1 && mode == SEQUENCE
        )
    }

    fun dependentDataChanged(keys: List<Trigger.Key>, @Trigger.Mode mode: Int): TriggerBehavior {
        val flags = applyToTriggerFlags(0)
        val extras = applyToTriggerExtras(listOf())

        return TriggerBehavior(keys, mode, flags, extras)
    }

    fun applyToTrigger(trigger: Trigger): Trigger {
        val newFlags = applyToTriggerFlags(trigger.flags)
        val newExtras = applyToTriggerExtras(trigger.extras)

        return trigger.clone(extras = newExtras, flags = newFlags)
    }

    fun setValue(id: String, value: Int): TriggerBehavior {
        when (id) {
            ID_VIBRATE_DURATION -> vibrateDuration.value = value
            ID_SEQUENCE_TRIGGER_TIMEOUT -> sequenceTriggerTimeout.value = value
            ID_LONG_PRESS_DELAY -> longPressDelay.value = value
            ID_DOUBLE_PRESS_DELAY -> doublePressDelay.value = value
        }

        return this
    }

    fun setValue(id: String, value: Boolean): TriggerBehavior {
        when (id) {
            ID_VIBRATE -> {
                vibrate.value = value

                vibrateDuration.isAllowed = value || longPressDoubleVibration.value
            }

            ID_LONG_PRESS_DOUBLE_VIBRATION -> {
                longPressDoubleVibration.value = value

                vibrateDuration.isAllowed = value || longPressDoubleVibration.value
            }

            ID_SCREEN_OFF_TRIGGER -> screenOffTrigger.value = value
        }

        return this
    }

    private fun applyToTriggerFlags(flags: Int): Int {
        return flags.applyBehaviorOption(vibrate, TRIGGER_FLAG_VIBRATE)
            .applyBehaviorOption(longPressDoubleVibration, TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION)
            .applyBehaviorOption(screenOffTrigger, TRIGGER_FLAG_SCREEN_OFF_TRIGGERS)
    }

    private fun applyToTriggerExtras(extras: List<Extra>): List<Extra> {
        return extras
            .applyBehaviorOption(vibrateDuration, EXTRA_VIBRATION_DURATION)
            .applyBehaviorOption(longPressDelay, EXTRA_LONG_PRESS_DELAY)
            .applyBehaviorOption(doublePressDelay, EXTRA_DOUBLE_PRESS_DELAY)
            .applyBehaviorOption(sequenceTriggerTimeout, EXTRA_SEQUENCE_TRIGGER_TIMEOUT)
    }
}

class ActionBehavior(
    action: Action,
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

    val repeatRate: BehaviorOption<Int>

    val repeatDelay: BehaviorOption<Int>

    val multiplier = BehaviorOption(
        id = ID_MULTIPLIER,
        value = action.extras.getData(Action.EXTRA_MULTIPLIER).valueOrNull()?.toInt() ?: BehaviorOption.DEFAULT,
        isAllowed = true
    )

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
            .applyBehaviorOption(holdDown, Action.ACTION_FLAG_HOLD_DOWN)

        val newExtras = action.extras
            .applyBehaviorOption(repeatRate, Action.EXTRA_REPEAT_RATE)
            .applyBehaviorOption(repeatDelay, Action.EXTRA_REPEAT_DELAY)
            .applyBehaviorOption(multiplier, Action.EXTRA_MULTIPLIER)

        newExtras.removeAll { it.id == Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR }
        if (stopRepeatingWhenTriggerPressedAgain.value) {
            newExtras.add(
                Extra(Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR, Action.STOP_REPEAT_BEHAVIOUR_TRIGGER_AGAIN.toString()))
        }

        return action.clone(flags = newFlags, extras = newExtras)
    }

    fun setValue(id: String, value: Int): ActionBehavior {
        when (id) {
            ID_REPEAT_RATE -> repeatRate.value = value
            ID_REPEAT_DELAY -> repeatDelay.value = value
            ID_MULTIPLIER -> multiplier.value = value
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
            }
        }

        return this
    }
}

class TriggerKeyBehavior(
    key: Trigger.Key,
    @Trigger.Mode mode: Int
) : Serializable {

    companion object {
        const val ID_DO_NOT_CONSUME_KEY_EVENT = "do_not_consume_key_event"
        const val ID_CLICK_TYPE = "click_type"
    }

    val uniqueId = key.uniqueId

    val clickType = BehaviorOption(
        id = ID_CLICK_TYPE,
        value = key.clickType,
        isAllowed = mode == SEQUENCE || mode == Trigger.UNDEFINED
    )

    val doNotConsumeKeyEvents = BehaviorOption(
        id = ID_DO_NOT_CONSUME_KEY_EVENT,
        value = key.flags.hasFlag(Trigger.Key.FLAG_DO_NOT_CONSUME_KEY_EVENT),
        isAllowed = true
    )

    fun setValue(id: String, value: Boolean): TriggerKeyBehavior {
        when (id) {
            ID_DO_NOT_CONSUME_KEY_EVENT -> doNotConsumeKeyEvents.value = value
        }

        return this
    }

    fun setValue(id: String, value: Int): TriggerKeyBehavior {
        when (id) {
            ID_CLICK_TYPE -> clickType.value = value
        }

        return this
    }

    fun applyToTriggerKey(key: Trigger.Key): Trigger.Key {
        key.flags = key.flags.applyBehaviorOption(doNotConsumeKeyEvents, Trigger.Key.FLAG_DO_NOT_CONSUME_KEY_EVENT)

        key.clickType = clickType.value

        return key
    }
}
