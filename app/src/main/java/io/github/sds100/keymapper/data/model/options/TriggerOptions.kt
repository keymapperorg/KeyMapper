package io.github.sds100.keymapper.data.model.options

import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.getData
import io.github.sds100.keymapper.data.model.options.BoolOption.Companion.saveBoolOption
import io.github.sds100.keymapper.data.model.options.IntOption.Companion.saveIntOption
import io.github.sds100.keymapper.util.KeyEventUtils
import io.github.sds100.keymapper.util.result.valueOrNull
import kotlinx.android.parcel.Parcelize
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 25/11/20.
 */

@Parcelize
class TriggerOptions(
    override val id: String = "trigger",
    private val vibrate: BoolOption,
    private val longPressDoubleVibration: BoolOption,
    private val screenOffTrigger: BoolOption,
    private val longPressDelay: IntOption,
    private val doublePressDelay: IntOption,
    private val vibrateDuration: IntOption,
    private val sequenceTriggerTimeout: IntOption
) : BaseOptions<Trigger> {

    companion object {
        const val ID_LONG_PRESS_DELAY = "long_press_delay"
        const val ID_DOUBLE_PRESS_DELAY = "double_press_delay"
        const val ID_SEQUENCE_TRIGGER_TIMEOUT = "sequence_trigger_timeout"
        const val ID_VIBRATE_DURATION = "vibrate_duration"
        const val ID_VIBRATE = "vibrate"
        const val ID_LONG_PRESS_DOUBLE_VIBRATION = "long_press_double_vibration"
        const val ID_SCREEN_OFF_TRIGGER = "screen_off_trigger"
    }

    constructor(trigger: Trigger) : this(
        vibrate = BoolOption(
            id = ID_VIBRATE,
            value = trigger.flags.hasFlag(Trigger.TRIGGER_FLAG_VIBRATE),
            isAllowed = true
        ),
        longPressDoubleVibration = BoolOption(
            id = ID_LONG_PRESS_DOUBLE_VIBRATION,
            value = trigger.flags.hasFlag(Trigger.TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION),
            isAllowed = (trigger.keys.size == 1 || (trigger.mode == Trigger.PARALLEL))
                && trigger.keys.getOrNull(0)?.clickType == Trigger.LONG_PRESS
        ),
        screenOffTrigger = BoolOption(
            id = ID_SCREEN_OFF_TRIGGER,
            value = trigger.flags.hasFlag(Trigger.TRIGGER_FLAG_SCREEN_OFF_TRIGGERS),
            isAllowed = trigger.keys.isNotEmpty() && trigger.keys.all {
                KeyEventUtils.GET_EVENT_LABEL_TO_KEYCODE.containsValue(it.keyCode)
            }
        ),

        longPressDelay = IntOption(
            id = ID_LONG_PRESS_DELAY,
            value = trigger.extras.getData(Trigger.EXTRA_LONG_PRESS_DELAY).valueOrNull()?.toInt()
                ?: IntOption.DEFAULT,
            isAllowed = trigger.keys.any { it.clickType == Trigger.LONG_PRESS }
        ),

        doublePressDelay = IntOption(
            id = ID_DOUBLE_PRESS_DELAY,
            value = trigger.extras.getData(Trigger.EXTRA_DOUBLE_PRESS_DELAY).valueOrNull()?.toInt()
                ?: IntOption.DEFAULT,
            isAllowed = trigger.keys.any { it.clickType == Trigger.DOUBLE_PRESS }
        ),

        vibrateDuration = IntOption(
            id = ID_VIBRATE_DURATION,
            value = trigger.extras.getData(Trigger.EXTRA_VIBRATION_DURATION).valueOrNull()?.toInt()
                ?: IntOption.DEFAULT,
            isAllowed = trigger.flags.hasFlag(Trigger.TRIGGER_FLAG_VIBRATE)
                || trigger.flags.hasFlag(Trigger.TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION)
        ),

        sequenceTriggerTimeout = IntOption(
            id = ID_SEQUENCE_TRIGGER_TIMEOUT,
            value =
            trigger.extras.getData(Trigger.EXTRA_SEQUENCE_TRIGGER_TIMEOUT).valueOrNull()?.toInt()
                ?: IntOption.DEFAULT,
            isAllowed = !trigger.keys.isNullOrEmpty()
                && trigger.keys.size > 1
                && trigger.mode == Trigger.SEQUENCE
        )
    )

    override val intOptions: List<IntOption>
        get() = listOf(
            longPressDelay,
            doublePressDelay,
            vibrateDuration,
            sequenceTriggerTimeout
        )

    override val boolOptions: List<BoolOption>
        get() = listOf(
            vibrate,
            longPressDoubleVibration,
            screenOffTrigger
        )

    override fun setValue(id: String, value: Int): TriggerOptions {
        when (id) {
            ID_VIBRATE_DURATION -> vibrateDuration.value = value
            ID_SEQUENCE_TRIGGER_TIMEOUT -> sequenceTriggerTimeout.value = value
            ID_LONG_PRESS_DELAY -> longPressDelay.value = value
            ID_DOUBLE_PRESS_DELAY -> doublePressDelay.value = value
        }

        return this
    }

    override fun setValue(id: String, value: Boolean): TriggerOptions {
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

    override fun apply(old: Trigger): Trigger {
        val newFlags = applyToTriggerFlags(old.flags)
        val newExtras = applyToTriggerExtras(old.extras)

        return old.copy(extras = newExtras, flags = newFlags)
    }

    fun dependentDataChanged(keys: List<Trigger.Key>, @Trigger.Mode mode: Int): TriggerOptions {
        val flags = applyToTriggerFlags(0)
        val extras = applyToTriggerExtras(listOf())

        return TriggerOptions(Trigger(keys, extras, mode, flags))
    }

    private fun applyToTriggerFlags(flags: Int): Int {
        return flags.saveBoolOption(vibrate, Trigger.TRIGGER_FLAG_VIBRATE)
            .saveBoolOption(longPressDoubleVibration, Trigger.TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION)
            .saveBoolOption(screenOffTrigger, Trigger.TRIGGER_FLAG_SCREEN_OFF_TRIGGERS)
    }

    private fun applyToTriggerExtras(extras: List<Extra>): List<Extra> {
        return extras
            .saveIntOption(vibrateDuration, Trigger.EXTRA_VIBRATION_DURATION)
            .saveIntOption(longPressDelay, Trigger.EXTRA_LONG_PRESS_DELAY)
            .saveIntOption(doublePressDelay, Trigger.EXTRA_DOUBLE_PRESS_DELAY)
            .saveIntOption(sequenceTriggerTimeout, Trigger.EXTRA_SEQUENCE_TRIGGER_TIMEOUT)
    }
}