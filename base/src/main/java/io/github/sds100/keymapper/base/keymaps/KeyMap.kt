package io.github.sds100.keymapper.base.keymaps

import android.view.KeyEvent
import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.ActionEntityMapper
import io.github.sds100.keymapper.base.actions.canBeHeldDown
import io.github.sds100.keymapper.base.constraints.ConstraintEntityMapper
import io.github.sds100.keymapper.base.constraints.ConstraintModeEntityMapper
import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.detection.KeyMapAlgorithm
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.base.trigger.TriggerEntityMapper
import io.github.sds100.keymapper.base.trigger.TriggerKey
import io.github.sds100.keymapper.data.entities.FloatingButtonEntityWithLayout
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class KeyMap(
    val dbId: Long? = null,
    val uid: String = UUID.randomUUID().toString(),
    val trigger: Trigger = Trigger(),
    val actionList: List<Action> = emptyList(),
    val constraintState: ConstraintState = ConstraintState(),
    val isEnabled: Boolean = true,
    val groupUid: String? = null,
) {

    val showToast: Boolean
        get() = trigger.showToast

    val vibrate: Boolean
        get() = trigger.vibrate

    val vibrateDuration: Int?
        get() = trigger.vibrateDuration

    fun isRepeatingActionsAllowed(): Boolean = KeyMapAlgorithm.performActionOnDown(trigger)

    fun isChangingActionRepeatRateAllowed(action: Action): Boolean =
        action.repeat && isRepeatingActionsAllowed()

    fun isChangingActionRepeatDelayAllowed(action: Action): Boolean =
        action.repeat && isRepeatingActionsAllowed()

    fun isHoldingDownActionAllowed(action: Action): Boolean =
        KeyMapAlgorithm.performActionOnDown(trigger) && action.data.canBeHeldDown()

    fun isHoldingDownActionBeforeRepeatingAllowed(action: Action): Boolean =
        action.repeat && action.holdDown

    fun isChangingRepeatModeAllowed(action: Action): Boolean =
        action.repeat && isRepeatingActionsAllowed()

    fun isChangingRepeatLimitAllowed(action: Action): Boolean =
        action.repeat && isRepeatingActionsAllowed()

    fun isStopHoldingDownActionWhenTriggerPressedAgainAllowed(action: Action): Boolean =
        action.holdDown && !action.repeat

    fun isDelayBeforeNextActionAllowed(): Boolean = actionList.isNotEmpty()
}

/**
 * Whether this key map requires an input method to detect the key events.
 * If the key map needs to answer or end a call then it must use an input method to detect
 * the key events because volume key events are not sent to accessibility services when a call
 * is incoming.
 */
fun KeyMap.requiresImeKeyEventForwarding(): Boolean {
    val hasPhoneCallAction =
        actionList.any { it.data is ActionData.AnswerCall || it.data is ActionData.EndCall }

    val hasVolumeKeys = trigger.keys
        .mapNotNull { it as? io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey }
        .any {
            it.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                it.keyCode == KeyEvent.KEYCODE_VOLUME_UP
        }

    return hasVolumeKeys && hasPhoneCallAction
}

/**
 * Whether this trigger key requires an input method to detect the key events.
 * If the key map needs to answer or end a call then it must use an input method to detect
 * the key events because volume key events are not sent to accessibility services when a call
 * is incoming.
 */
fun KeyMap.requiresImeKeyEventForwardingInPhoneCall(triggerKey: TriggerKey): Boolean {
    if (triggerKey !is io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey) {
        return false
    }

    val hasPhoneCallAction =
        actionList.any { it.data is ActionData.AnswerCall || it.data is ActionData.EndCall }

    val hasVolumeKeys = trigger.keys
        .mapNotNull { it as? io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey }
        .any {
            it.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                it.keyCode == KeyEvent.KEYCODE_VOLUME_UP
        }

    return hasVolumeKeys && hasPhoneCallAction
}

object KeyMapEntityMapper {
    fun fromEntity(
        entity: KeyMapEntity,
        floatingButtons: List<FloatingButtonEntityWithLayout>,
    ): KeyMap {
        val actionList = entity.actionList
            .filterNotNull()
            .mapNotNull { ActionEntityMapper.fromEntity(it) }

        val constraintList =
            entity.constraintList.map { ConstraintEntityMapper.fromEntity(it) }.toSet()

        val constraintMode = ConstraintModeEntityMapper.fromEntity(entity.constraintMode)

        return KeyMap(
            dbId = entity.id,
            uid = entity.uid,
            trigger = TriggerEntityMapper.fromEntity(entity.trigger, floatingButtons),
            actionList = actionList,
            constraintState = ConstraintState(constraintList, constraintMode),
            isEnabled = entity.isEnabled,
            groupUid = entity.groupUid,
        )
    }

    fun toEntity(keyMap: KeyMap, dbId: Long): KeyMapEntity {
        val actionEntityList = ActionEntityMapper.toEntity(keyMap)

        return KeyMapEntity(
            id = dbId,
            trigger = TriggerEntityMapper.toEntity(keyMap.trigger),
            actionList = actionEntityList,
            constraintList = keyMap.constraintState.constraints.map {
                ConstraintEntityMapper.toEntity(
                    it,
                )
            },
            constraintMode = ConstraintModeEntityMapper.toEntity(keyMap.constraintState.mode),
            isEnabled = keyMap.isEnabled,
            uid = keyMap.uid,
            groupUid = keyMap.groupUid,
        )
    }
}
