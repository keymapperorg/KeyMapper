package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.common.result.success
import io.github.sds100.keymapper.common.result.then
import io.github.sds100.keymapper.common.result.valueOrNull
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.keymaps.KeyMap
import kotlinx.serialization.Serializable
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag
import java.util.UUID

@Serializable
data class Action(
    val uid: String = UUID.randomUUID().toString(),
    val data: ActionData,

    val repeat: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.TRIGGER_RELEASED,
    val repeatRate: Int? = null,
    val repeatDelay: Int? = null,
    val repeatLimit: Int? = null,

    val holdDown: Boolean = false,
    val stopHoldDownWhenTriggerPressedAgain: Boolean = false,
    val holdDownDuration: Int? = null,

    val multiplier: Int? = null,
    val delayBeforeNextAction: Int? = null,
)

object ActionEntityMapper {
    fun fromEntity(entity: ActionEntity): Action? {
        val data = ActionDataEntityMapper.fromEntity(entity) ?: return null

        val stopHoldDownWhenTriggerPressedAgain: Boolean =
            entity.extras.getData(ActionEntity.EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR).then {
                (it == ActionEntity.STOP_HOLD_DOWN_BEHAVIOR_TRIGGER_PRESSED_AGAIN.toString()).success()
            }.valueOrNull() == true

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

        val repeatMode: RepeatMode = if (entity.flags.hasFlag(ActionEntity.ACTION_FLAG_REPEAT)) {
            val repeatBehaviourExtra =
                entity.extras.getData(ActionEntity.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR)
                    .valueOrNull()
                    ?.toIntOrNull()

            when (repeatBehaviourExtra) {
                ActionEntity.STOP_REPEAT_BEHAVIOUR_LIMIT_REACHED -> RepeatMode.LIMIT_REACHED
                ActionEntity.STOP_REPEAT_BEHAVIOUR_TRIGGER_PRESSED_AGAIN -> RepeatMode.TRIGGER_PRESSED_AGAIN
                else -> RepeatMode.TRIGGER_RELEASED
            }
        } else {
            RepeatMode.TRIGGER_RELEASED
        }

        val repeatLimit = entity.extras
            .getData(ActionEntity.EXTRA_REPEAT_LIMIT)
            .valueOrNull()
            ?.toIntOrNull()

        return Action(
            uid = entity.uid,
            data = data,
            repeat = entity.flags.hasFlag(ActionEntity.ACTION_FLAG_REPEAT),
            repeatMode = repeatMode,
            holdDown = entity.flags.hasFlag(ActionEntity.ACTION_FLAG_HOLD_DOWN),
            stopHoldDownWhenTriggerPressedAgain = stopHoldDownWhenTriggerPressedAgain,
            repeatRate = repeatRate,
            repeatDelay = repeatDelay,
            repeatLimit = repeatLimit,
            holdDownDuration = holdDownDuration,
            delayBeforeNextAction = delayBeforeNextAction,
            multiplier = multiplier,
        )
    }

    fun toEntity(keyMap: KeyMap): List<ActionEntity> = keyMap.actionList.map { action ->
        val base = ActionDataEntityMapper.toEntity(action.data)

        val extras = mutableListOf<EntityExtra>().apply {
            if (keyMap.isDelayBeforeNextActionAllowed() && action.delayBeforeNextAction != null) {
                add(
                    EntityExtra(
                        ActionEntity.EXTRA_DELAY_BEFORE_NEXT_ACTION,
                        action.delayBeforeNextAction.toString(),
                    ),
                )
            }

            if (action.multiplier != null) {
                add(EntityExtra(ActionEntity.EXTRA_MULTIPLIER, action.multiplier.toString()))
            }

            if (keyMap.isHoldingDownActionBeforeRepeatingAllowed(action) && action.holdDownDuration != null) {
                add(
                    EntityExtra(
                        ActionEntity.EXTRA_HOLD_DOWN_DURATION,
                        action.holdDownDuration.toString(),
                    ),
                )
            }

            if (keyMap.isChangingActionRepeatRateAllowed(action) && action.repeatRate != null) {
                add(EntityExtra(ActionEntity.EXTRA_REPEAT_RATE, action.repeatRate.toString()))
            }

            if (keyMap.isChangingActionRepeatDelayAllowed(action) && action.repeatDelay != null) {
                add(EntityExtra(ActionEntity.EXTRA_REPEAT_DELAY, action.repeatDelay.toString()))
            }

            if (keyMap.isChangingRepeatLimitAllowed(action) && action.repeatLimit != null) {
                add(EntityExtra(ActionEntity.EXTRA_REPEAT_LIMIT, action.repeatLimit.toString()))
            }

            if (keyMap.isChangingRepeatModeAllowed(action) && action.repeatMode == RepeatMode.TRIGGER_PRESSED_AGAIN
            ) {
                add(
                    EntityExtra(
                        ActionEntity.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR,
                        ActionEntity.STOP_REPEAT_BEHAVIOUR_TRIGGER_PRESSED_AGAIN.toString(),
                    ),
                )
            }

            if (keyMap.isChangingRepeatModeAllowed(action) && action.repeatMode == RepeatMode.LIMIT_REACHED) {
                add(
                    EntityExtra(
                        ActionEntity.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR,
                        ActionEntity.STOP_REPEAT_BEHAVIOUR_LIMIT_REACHED.toString(),
                    ),
                )
            }

            if (keyMap.isStopHoldingDownActionWhenTriggerPressedAgainAllowed(action) &&
                action.stopHoldDownWhenTriggerPressedAgain
            ) {
                add(
                    EntityExtra(
                        ActionEntity.EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR,
                        ActionEntity.STOP_HOLD_DOWN_BEHAVIOR_TRIGGER_PRESSED_AGAIN.toString(),
                    ),
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

        return@map ActionEntity(
            type = base.type,
            data = base.data,
            extras = base.extras.plus(extras),
            flags = base.flags.withFlag(flags),
            uid = action.uid,
        )
    }
}
