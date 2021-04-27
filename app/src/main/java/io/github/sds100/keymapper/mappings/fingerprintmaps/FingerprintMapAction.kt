package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.actions.Action
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.ActionDataEntityMapper
import io.github.sds100.keymapper.mappings.isDelayBeforeNextActionAllowed
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.serialization.Serializable
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag
import java.util.*

/**
 * Created by sds100 on 09/03/2021.
 */

@Serializable
data class FingerprintMapAction(
    override val uid: String = UUID.randomUUID().toString(),
    override val data: ActionData,
    override val delayBeforeNextAction: Int? = null,
    override val multiplier: Int? = null,
    val repeatUntilSwipedAgain: Boolean = false,
    override val repeatRate: Int? = null,
    val holdDownUntilSwipedAgain: Boolean = false,
    override val holdDownDuration: Int? = null
) : Action {
    override val repeat: Boolean
        get() = repeatUntilSwipedAgain

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

        return FingerprintMapAction(
            uid = entity.uid,
            data = data,
            repeatUntilSwipedAgain = entity.flags.hasFlag(ActionEntity.ACTION_FLAG_REPEAT),
            holdDownUntilSwipedAgain = entity.flags.hasFlag(ActionEntity.ACTION_FLAG_HOLD_DOWN),
            repeatRate = repeatRate,
            holdDownDuration = holdDownDuration,
            delayBeforeNextAction = delayBeforeNextAction,
            multiplier = multiplier
        )
    }

    fun toEntity(fingerprintMap: FingerprintMap): List<ActionEntity> =
        fingerprintMap.actionList.mapNotNull { action ->
            val base = ActionDataEntityMapper.toEntity(action.data) ?: return@mapNotNull null

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

                if (fingerprintMap.isChangingActionRepeatRateAllowed(action) && action.repeatRate != null) {
                    add(Extra(ActionEntity.EXTRA_REPEAT_RATE, action.repeatRate.toString()))
                }
            }

            var flags = 0

            if (fingerprintMap.isRepeatingActionUntilSwipedAgainAllowed() && action.repeatUntilSwipedAgain) {
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