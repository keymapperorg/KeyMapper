package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.actions.Action
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.ActionDataEntityMapper
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.mappings.isDelayBeforeNextActionAllowed
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.serialization.Serializable
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag
import java.util.UUID

/**
 * Created by sds100 on 09/03/2021.
 */

@Serializable
data class FingerprintMapAction(
    override val uid: String = UUID.randomUUID().toString(),
    override val data: ActionData,
    override val delayBeforeNextAction: Int? = null,
    override val multiplier: Int? = null,
    override val repeat: Boolean = false,
    override val repeatRate: Int? = null,
    override val repeatLimit: Int? = null,
    override val repeatMode: RepeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,

    val holdDownUntilSwipedAgain: Boolean = false,
    override val holdDownDuration: Int? = null
) : Action {
    override val holdDown: Boolean
        get() = holdDownUntilSwipedAgain
}

object FingerprintMapActionEntityMapper {
    fun fromEntity(entity: ActionEntity): FingerprintMapAction? {
        val data = ActionDataEntityMapper.fromEntity(entity) ?: return null

        val repeatRate =
            entity.extras.getData(ActionEntity.EXTRA_REPEAT_RATE).valueOrNull()?.toIntOrNull()

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

        val repeatLimit = entity.extras
            .getData(ActionEntity.EXTRA_REPEAT_LIMIT)
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
                else -> RepeatMode.TRIGGER_PRESSED_AGAIN //Don't allow until released
            }
        } else {
            RepeatMode.TRIGGER_PRESSED_AGAIN
        }

        return FingerprintMapAction(
            uid = entity.uid,
            data = data,
            repeat = entity.flags.hasFlag(ActionEntity.ACTION_FLAG_REPEAT),
            repeatMode = repeatMode,
            holdDownUntilSwipedAgain = entity.flags.hasFlag(ActionEntity.ACTION_FLAG_HOLD_DOWN),
            repeatRate = repeatRate,
            repeatLimit = repeatLimit,
            holdDownDuration = holdDownDuration,
            delayBeforeNextAction = delayBeforeNextAction,
            multiplier = multiplier
        )
    }

    fun toEntity(fingerprintMap: FingerprintMap): List<ActionEntity> =
        fingerprintMap.actionList.mapNotNull { action ->
            val base = ActionDataEntityMapper.toEntity(action.data)

            val extras = mutableListOf<Extra>().apply {
                if (fingerprintMap.isDelayBeforeNextActionAllowed() && action.delayBeforeNextAction != null) {
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

                if (fingerprintMap.isHoldingDownActionBeforeRepeatingAllowed(action) && action.holdDownDuration != null) {
                    add(
                        Extra(
                            ActionEntity.EXTRA_HOLD_DOWN_DURATION,
                            action.holdDownDuration.toString()
                        )
                    )
                }

                if (fingerprintMap.isChangingRepeatModeAllowed(action) && action.repeatMode == RepeatMode.TRIGGER_PRESSED_AGAIN
                ) {
                    add(
                        Extra(
                            ActionEntity.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR,
                            ActionEntity.STOP_REPEAT_BEHAVIOUR_TRIGGER_PRESSED_AGAIN.toString()
                        )
                    )
                }

                if (fingerprintMap.isChangingRepeatModeAllowed(action) && action.repeatMode == RepeatMode.LIMIT_REACHED) {
                    add(
                        Extra(
                            ActionEntity.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR,
                            ActionEntity.STOP_REPEAT_BEHAVIOUR_LIMIT_REACHED.toString()
                        )
                    )
                }

                if (fingerprintMap.isChangingActionRepeatRateAllowed(action) && action.repeatRate != null) {
                    add(Extra(ActionEntity.EXTRA_REPEAT_RATE, action.repeatRate.toString()))
                }

                if (fingerprintMap.isChangingRepeatLimitAllowed(action) && action.repeatLimit != null) {
                    add(Extra(ActionEntity.EXTRA_REPEAT_LIMIT, action.repeatLimit.toString()))
                }
            }

            var flags = 0

            if (fingerprintMap.isRepeatingActionsAllowed() && action.repeat) {
                flags = flags.withFlag(ActionEntity.ACTION_FLAG_REPEAT)
            }

            if (fingerprintMap.isHoldingDownActionUntilSwipedAgainAllowed(action) && action.holdDownUntilSwipedAgain) {
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