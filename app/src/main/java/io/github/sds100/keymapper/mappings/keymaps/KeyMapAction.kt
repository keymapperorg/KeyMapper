package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.actions.Action
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.ActionDataEntityMapper
import io.github.sds100.keymapper.mappings.isDelayBeforeNextActionAllowed
import io.github.sds100.keymapper.util.success
import io.github.sds100.keymapper.util.then
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.serialization.Serializable
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag
import java.util.*

/**
 * Created by sds100 on 09/03/2021.
 */

@Serializable
data class KeyMapAction(
    override val uid: String = UUID.randomUUID().toString(),
    override val data: ActionData,
    override val repeat: Boolean = false,
    override val holdDown: Boolean = false,
    val stopRepeatingWhenTriggerPressedAgain: Boolean = false,
    val stopHoldDownWhenTriggerPressedAgain: Boolean = false,
    override val repeatRate: Int? = null,
    val repeatDelay: Int? = null,
    override val holdDownDuration: Int? = null,
    override val multiplier: Int? = null,
    override val delayBeforeNextAction: Int? = null
) : Action {
    companion object {
        const val REPEAT_DELAY_MIN = 0
    }
}

object KeymapActionEntityMapper {
    fun fromEntity(entity: ActionEntity): KeyMapAction? {
        val data = ActionDataEntityMapper.fromEntity(entity) ?: return null

        val stopRepeatingWhenTriggerPressedAgain: Boolean =
            entity.extras.getData(ActionEntity.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR).then {
                (it == ActionEntity.STOP_REPEAT_BEHAVIOUR_TRIGGER_PRESSED_AGAIN.toString()).success()
            }.valueOrNull() ?: false

        val stopHoldDownWhenTriggerPressedAgain: Boolean =
            entity.extras.getData(ActionEntity.EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR).then {
                (it == ActionEntity.STOP_HOLD_DOWN_BEHAVIOR_TRIGGER_PRESSED_AGAIN.toString()).success()
            }.valueOrNull() ?: false

        val repeatRate =
            entity.extras.getData(ActionEntity.EXTRA_REPEAT_RATE).valueOrNull()?.toIntOrNull()

        val repeatDelay =
            entity.extras.getData(ActionEntity.EXTRA_REPEAT_DELAY).valueOrNull()?.toIntOrNull()

        val holdDownDuration =
            entity.extras
                .getData(ActionEntity.EXTRA_HOLD_DOWN_DURATION)
                .valueOrNull()
                ?.toIntOrNull()

        val delayBeforeNextAction =
            entity.extras
                .getData(ActionEntity.EXTRA_DELAY_BEFORE_NEXT_ACTION)
                .valueOrNull()
                ?.toIntOrNull()

        val multiplier =
            entity.extras
                .getData(ActionEntity.EXTRA_MULTIPLIER)
                .valueOrNull()
                ?.toIntOrNull()

        return KeyMapAction(
            uid = entity.uid,
            data = data,
            repeat = entity.flags.hasFlag(ActionEntity.ACTION_FLAG_REPEAT),
            holdDown = entity.flags.hasFlag(ActionEntity.ACTION_FLAG_HOLD_DOWN),
            stopRepeatingWhenTriggerPressedAgain = stopRepeatingWhenTriggerPressedAgain,
            stopHoldDownWhenTriggerPressedAgain = stopHoldDownWhenTriggerPressedAgain,
            repeatRate = repeatRate,
            repeatDelay = repeatDelay,
            holdDownDuration = holdDownDuration,
            delayBeforeNextAction = delayBeforeNextAction,
            multiplier = multiplier
        )
    }

    fun toEntity(keyMap: KeyMap): List<ActionEntity> = keyMap.actionList.mapNotNull { action ->
        val base = ActionDataEntityMapper.toEntity(action.data) ?: return@mapNotNull null

        val extras = mutableListOf<Extra>().apply {
            if (keyMap.isDelayBeforeNextActionAllowed() && action.delayBeforeNextAction != null) {
                add(
                    Extra(
                        ActionEntity.EXTRA_DELAY_BEFORE_NEXT_ACTION,
                        action.delayBeforeNextAction.toString()
                    )
                )
            }

            if (action.multiplier != null) {
                add(Extra(ActionEntity.EXTRA_MULTIPLIER, action.multiplier.toString()))
            }

            if (keyMap.isHoldingDownActionBeforeRepeatingAllowed(action) && action.holdDownDuration != null) {
                add(
                    Extra(
                        ActionEntity.EXTRA_HOLD_DOWN_DURATION,
                        action.holdDownDuration.toString()
                    )
                )
            }

            if (keyMap.isChangingActionRepeatRateAllowed(action) && action.repeatRate != null) {
                add(Extra(ActionEntity.EXTRA_REPEAT_RATE, action.repeatRate.toString()))
            }

            if (keyMap.isChangingActionRepeatDelayAllowed(action) && action.repeatDelay != null) {
                add(Extra(ActionEntity.EXTRA_REPEAT_DELAY, action.repeatDelay.toString()))
            }

            if (keyMap.isStopRepeatingActionWhenTriggerPressedAgainAllowed(action)
                && action.stopRepeatingWhenTriggerPressedAgain
            ) {
                add(
                    Extra(
                        ActionEntity.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR,
                        ActionEntity.STOP_REPEAT_BEHAVIOUR_TRIGGER_PRESSED_AGAIN.toString()
                    )
                )
            }


            if (keyMap.isStopHoldingDownActionWhenTriggerPressedAgainAllowed(action)
                && action.stopHoldDownWhenTriggerPressedAgain
            ) {
                add(
                    Extra(
                        ActionEntity.EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR,
                        ActionEntity.STOP_HOLD_DOWN_BEHAVIOR_TRIGGER_PRESSED_AGAIN.toString()
                    )
                )
            }
        }

        var flags = 0

        if (keyMap.isRepeatingActionsAllowed() && action.repeat) {
            flags = flags.withFlag(ActionEntity.ACTION_FLAG_REPEAT)
        }

        if (keyMap.isHoldingDownActionAllowed(action) && action.holdDown) {
            flags = flags.withFlag(ActionEntity.ACTION_FLAG_HOLD_DOWN)
        }

        return@mapNotNull ActionEntity(
            type = base.type,
            data = base.data,
            extras = base.extras.plus(extras),
            flags = base.flags.withFlag(flags),
            uid = action.uid
        )
    }
}