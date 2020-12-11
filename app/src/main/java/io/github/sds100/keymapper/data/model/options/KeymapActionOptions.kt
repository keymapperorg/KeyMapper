package io.github.sds100.keymapper.data.model.options

import android.os.Build
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.options.BoolOption.Companion.saveBoolOption
import io.github.sds100.keymapper.data.model.options.IntOption.Companion.saveIntOption
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.KeymapDetectionDelegate
import kotlinx.android.parcel.Parcelize

/**
 * Created by sds100 on 22/11/20.
 */

@Parcelize
class KeymapActionOptions(
    override val id: String,
    val repeat: BoolOption,
    private val showVolumeUi: BoolOption,
    private val showPerformingActionToast: BoolOption,
    val holdDown: BoolOption,
    val stopRepeatingWhenTriggerReleased: BoolOption,
    val stopRepeatingWhenTriggerPressedAgain: BoolOption,
    val stopHoldDownWhenTriggerReleased: BoolOption,
    val stopHoldDownWhenTriggerPressedAgain: BoolOption,

    private val repeatRate: IntOption,
    private val repeatDelay: IntOption,
    private val holdDownDuration: IntOption,
    private val delayBeforeNextAction: IntOption,
    private val multiplier: IntOption

) : BaseOptions<Action> {

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
        const val ID_HOLD_DOWN_DURATION = "hold_down_duration"
    }

    constructor(action: Action,
                actionCount: Int,
                @Trigger.Mode triggerMode: Int? = null,
                triggerKeys: List<Trigger.Key>? = null) : this(
        id = action.uid,

        showVolumeUi = BoolOption(
            id = ID_SHOW_VOLUME_UI,
            value = action.showVolumeUi,
            isAllowed = ActionUtils.isVolumeAction(action.data)
        ),

        repeat = BoolOption(
            id = ID_REPEAT,
            value = action.repeat,
            isAllowed = if (triggerKeys != null && triggerMode != null) {
                KeymapDetectionDelegate.performActionOnDown(triggerKeys, triggerMode)
            } else {
                false
            }
        ),

        showPerformingActionToast = BoolOption(
            id = ID_SHOW_PERFORMING_ACTION_TOAST,
            value = action.showPerformingActionToast,
            isAllowed = true
        ),

        holdDown = BoolOption(
            id = ID_HOLD_DOWN,
            value = action.holdDown,
            isAllowed = if (triggerKeys != null && triggerMode != null) {
                KeymapDetectionDelegate.performActionOnDown(triggerKeys, triggerMode)
                    && (action.type == ActionType.KEY_EVENT
                    || (action.type == ActionType.TAP_COORDINATE
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    )
            } else {
                false
            }
        ),

        multiplier = IntOption(
            id = ID_MULTIPLIER,
            value = action.multiplier ?: IntOption.DEFAULT,
            isAllowed = true
        ),

        holdDownDuration = IntOption(
            id = ID_HOLD_DOWN_DURATION,
            value = action.holdDownDuration ?: IntOption.DEFAULT,
            isAllowed = action.repeat && action.holdDown
        ),

        repeatRate = IntOption(
            id = ID_REPEAT_RATE,
            value = action.repeatRate ?: IntOption.DEFAULT,
            isAllowed = action.repeat
        ),

        repeatDelay = IntOption(
            id = ID_REPEAT_DELAY,
            value = action.repeatDelay ?: IntOption.DEFAULT,
            isAllowed = action.repeat
        ),

        stopRepeatingWhenTriggerPressedAgain = BoolOption(
            id = ID_STOP_REPEATING_TRIGGER_PRESSED_AGAIN,
            value = action.stopRepeatingWhenTriggerPressedAgain,
            isAllowed = action.repeat
        ),

        stopRepeatingWhenTriggerReleased = BoolOption(
            id = ID_STOP_REPEATING_TRIGGER_RELEASED,
            value = action.stopRepeatingWhenTriggerReleased,
            isAllowed = action.repeat
        ),

        stopHoldDownWhenTriggerPressedAgain = BoolOption(
            id = ID_STOP_HOLD_DOWN_WHEN_TRIGGER_PRESSED_AGAIN,
            value = action.stopHoldDownWhenTriggerPressedAgain,
            isAllowed = action.holdDown && !action.repeat
        ),

        stopHoldDownWhenTriggerReleased = BoolOption(
            id = ID_STOP_HOLD_DOWN_WHEN_TRIGGER_RELEASED,
            value = action.stopHoldDownWhenTriggerReleased,
            isAllowed = action.holdDown
        ),

        delayBeforeNextAction = IntOption(
            id = ID_DELAY_BEFORE_NEXT_ACTION,
            value = action.delayBeforeNextAction ?: IntOption.DEFAULT,
            isAllowed = actionCount > 0
        )
    )

    override val intOptions = listOf(
        repeatRate,
        repeatDelay,
        delayBeforeNextAction,
        holdDownDuration,
        multiplier
    )

    override val boolOptions = listOf(
        showPerformingActionToast,
        showVolumeUi,
        repeat,
        holdDown
    )

    override fun setValue(id: String, value: Int): BaseOptions<Action> {
        when (id) {
            ID_REPEAT_RATE -> repeatRate.value = value
            ID_REPEAT_DELAY -> repeatDelay.value = value
            ID_MULTIPLIER -> multiplier.value = value
            ID_DELAY_BEFORE_NEXT_ACTION -> delayBeforeNextAction.value = value
            ID_HOLD_DOWN_DURATION -> holdDownDuration.value = value
        }

        return this
    }

    override fun setValue(id: String, value: Boolean): BaseOptions<Action> {
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

                holdDownDuration.isAllowed = holdDown.value && repeat.value
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
                holdDownDuration.isAllowed = holdDown.value && repeat.value
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

    override fun apply(old: Action): Action {
        val newFlags = old.flags
            .saveBoolOption(repeat, Action.ACTION_FLAG_REPEAT)
            .saveBoolOption(showVolumeUi, Action.ACTION_FLAG_SHOW_VOLUME_UI)
            .saveBoolOption(showPerformingActionToast, Action.ACTION_FLAG_SHOW_PERFORMING_ACTION_TOAST)
            .saveBoolOption(holdDown, Action.ACTION_FLAG_HOLD_DOWN)

        val newExtras = old.extras
            .saveIntOption(repeatRate, Action.EXTRA_REPEAT_RATE)
            .saveIntOption(repeatDelay, Action.EXTRA_REPEAT_DELAY)
            .saveIntOption(multiplier, Action.EXTRA_MULTIPLIER)
            .saveIntOption(delayBeforeNextAction, Action.EXTRA_DELAY_BEFORE_NEXT_ACTION)
            .saveIntOption(holdDownDuration, Action.EXTRA_HOLD_DOWN_DURATION)

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

        return old.copy(flags = newFlags, extras = newExtras, uid = old.uid)
    }
}