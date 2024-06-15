package io.github.sds100.keymapper.mappings.keymaps

import android.view.KeyEvent
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.canBeHeldDown
import io.github.sds100.keymapper.constraints.ConstraintEntityMapper
import io.github.sds100.keymapper.constraints.ConstraintModeEntityMapper
import io.github.sds100.keymapper.constraints.ConstraintState
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.mappings.Mapping
import io.github.sds100.keymapper.mappings.keymaps.detection.KeyMapController
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapTrigger
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeymapTriggerEntityMapper
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Created by sds100 on 03/03/2021.
 */

@Serializable
data class KeyMap(
    val dbId: Long? = null,
    val uid: String = UUID.randomUUID().toString(),
    val trigger: KeyMapTrigger = KeyMapTrigger(),
    override val actionList: List<KeyMapAction> = emptyList(),
    override val constraintState: ConstraintState = ConstraintState(),
    override val isEnabled: Boolean = true,
) : Mapping<KeyMapAction> {

    override val showToast: Boolean
        get() = trigger.showToast

    override val vibrate: Boolean
        get() = trigger.vibrate

    override val vibrateDuration: Int?
        get() = trigger.vibrateDuration

    fun isRepeatingActionsAllowed(): Boolean = KeyMapController.performActionOnDown(trigger)

    fun isChangingActionRepeatRateAllowed(action: KeyMapAction): Boolean =
        action.repeat && isRepeatingActionsAllowed()

    fun isChangingActionRepeatDelayAllowed(action: KeyMapAction): Boolean =
        action.repeat && isRepeatingActionsAllowed()

    fun isHoldingDownActionAllowed(action: KeyMapAction): Boolean =
        KeyMapController.performActionOnDown(trigger) && action.data.canBeHeldDown()

    fun isHoldingDownActionBeforeRepeatingAllowed(action: KeyMapAction): Boolean =
        action.repeat && action.holdDown

    fun isChangingRepeatModeAllowed(action: KeyMapAction): Boolean =
        action.repeat && isRepeatingActionsAllowed()

    fun isChangingRepeatLimitAllowed(action: KeyMapAction): Boolean =
        action.repeat && isRepeatingActionsAllowed()

    fun isStopHoldingDownActionWhenTriggerPressedAgainAllowed(action: KeyMapAction): Boolean =
        action.holdDown && !action.repeat
}

/**
 * @return whether this key map requires an input method to send the key events
 * because otherwise it won't be detected.
 */
fun KeyMap.requiresImeKeyEventForwarding(): Boolean =
    trigger.keys.any { it.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || it.keyCode == KeyEvent.KEYCODE_VOLUME_UP } &&
        actionList.any { it.data is ActionData.AnswerCall || it.data is ActionData.EndCall }

object KeyMapEntityMapper {
    fun fromEntity(entity: KeyMapEntity): KeyMap {
        val actionList = entity.actionList.mapNotNull { KeymapActionEntityMapper.fromEntity(it) }

        val constraintList =
            entity.constraintList.map { ConstraintEntityMapper.fromEntity(it) }.toSet()

        val constraintMode = ConstraintModeEntityMapper.fromEntity(entity.constraintMode)

        return KeyMap(
            dbId = entity.id,
            uid = entity.uid,
            trigger = KeymapTriggerEntityMapper.fromEntity(entity.trigger),
            actionList = actionList,
            constraintState = ConstraintState(constraintList, constraintMode),
            isEnabled = entity.isEnabled,
        )
    }

    fun toEntity(keyMap: KeyMap, dbId: Long): KeyMapEntity {
        val actionEntityList = KeymapActionEntityMapper.toEntity(keyMap)

        return KeyMapEntity(
            id = dbId,
            trigger = KeymapTriggerEntityMapper.toEntity(keyMap.trigger),
            actionList = actionEntityList,
            constraintList = keyMap.constraintState.constraints.map {
                ConstraintEntityMapper.toEntity(
                    it,
                )
            },
            constraintMode = ConstraintModeEntityMapper.toEntity(keyMap.constraintState.mode),
            isEnabled = keyMap.isEnabled,
            uid = keyMap.uid,
        )
    }
}
