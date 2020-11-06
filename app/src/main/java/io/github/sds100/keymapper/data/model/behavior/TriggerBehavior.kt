package io.github.sds100.keymapper.data.model.behavior

import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.behavior.BehaviorOption.Companion.applyBehaviorOption
import io.github.sds100.keymapper.data.model.getData
import io.github.sds100.keymapper.util.KeyEventUtils
import io.github.sds100.keymapper.util.result.valueOrNull
import splitties.bitflags.hasFlag

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
        value = flags.hasFlag(Trigger.TRIGGER_FLAG_VIBRATE),
        isAllowed = true
    )

    val longPressDoubleVibration = BehaviorOption(
        id = ID_LONG_PRESS_DOUBLE_VIBRATION,
        value = flags.hasFlag(Trigger.TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION),
        isAllowed = (keys.size == 1 || (mode == Trigger.PARALLEL))
            && keys.getOrNull(0)?.clickType == Trigger.LONG_PRESS
    )

    val screenOffTrigger = BehaviorOption(
        id = ID_SCREEN_OFF_TRIGGER,
        value = flags.hasFlag(Trigger.TRIGGER_FLAG_SCREEN_OFF_TRIGGERS),
        isAllowed = keys.isNotEmpty() && keys.all {
            KeyEventUtils.GET_EVENT_LABEL_TO_KEYCODE.containsValue(it.keyCode)
        }
    )

    val longPressDelay: BehaviorOption<Int>
    val doublePressDelay: BehaviorOption<Int>
    val vibrateDuration: BehaviorOption<Int>
    val sequenceTriggerTimeout: BehaviorOption<Int>

    /*
     It is very important that any new options are only allowed with a valid combination of other options. Make sure
     the "isAllowed" property considers all the other options.
     */

    init {

        val longPressDelayValue = extras.getData(Trigger.EXTRA_LONG_PRESS_DELAY).valueOrNull()?.toInt()

        longPressDelay = BehaviorOption(
            id = ID_LONG_PRESS_DELAY,
            value = longPressDelayValue ?: BehaviorOption.DEFAULT,
            isAllowed = keys.any { it.clickType == Trigger.LONG_PRESS }
        )

        val doublePressDelayValue = extras.getData(Trigger.EXTRA_DOUBLE_PRESS_DELAY).valueOrNull()?.toInt()

        doublePressDelay = BehaviorOption(
            id = ID_DOUBLE_PRESS_DELAY,
            value = doublePressDelayValue ?: BehaviorOption.DEFAULT,
            isAllowed = keys.any { it.clickType == Trigger.DOUBLE_PRESS }
        )

        val vibrateDurationValue = extras.getData(Trigger.EXTRA_VIBRATION_DURATION).valueOrNull()?.toInt()

        vibrateDuration = BehaviorOption(
            id = ID_VIBRATE_DURATION,
            value = vibrateDurationValue ?: BehaviorOption.DEFAULT,
            isAllowed = vibrate.value || longPressDoubleVibration.value
        )

        val sequenceTriggerTimeoutValue =
            extras.getData(Trigger.EXTRA_SEQUENCE_TRIGGER_TIMEOUT).valueOrNull()?.toInt()

        sequenceTriggerTimeout = BehaviorOption(
            id = ID_SEQUENCE_TRIGGER_TIMEOUT,
            value = sequenceTriggerTimeoutValue ?: BehaviorOption.DEFAULT,
            isAllowed = !keys.isNullOrEmpty() && keys.size > 1 && mode == Trigger.SEQUENCE
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
        return flags.applyBehaviorOption(vibrate, Trigger.TRIGGER_FLAG_VIBRATE)
            .applyBehaviorOption(longPressDoubleVibration, Trigger.TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION)
            .applyBehaviorOption(screenOffTrigger, Trigger.TRIGGER_FLAG_SCREEN_OFF_TRIGGERS)
    }

    private fun applyToTriggerExtras(extras: List<Extra>): List<Extra> {
        return extras
            .applyBehaviorOption(vibrateDuration, Trigger.EXTRA_VIBRATION_DURATION)
            .applyBehaviorOption(longPressDelay, Trigger.EXTRA_LONG_PRESS_DELAY)
            .applyBehaviorOption(doublePressDelay, Trigger.EXTRA_DOUBLE_PRESS_DELAY)
            .applyBehaviorOption(sequenceTriggerTimeout, Trigger.EXTRA_SEQUENCE_TRIGGER_TIMEOUT)
    }
}