package io.github.sds100.keymapper.mappings.keymaps.detection

import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.mappings.DisplaySimpleMappingUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapTriggerError
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

/**
 * Created by sds100 on 04/04/2021.
 */

class DisplayKeyMapUseCaseImpl(
        private val permissionAdapter: PermissionAdapter,
        private val inputMethodAdapter: InputMethodAdapter,
        displaySimpleMappingUseCase: DisplaySimpleMappingUseCase
) : DisplayKeyMapUseCase, DisplaySimpleMappingUseCase by displaySimpleMappingUseCase {
    private companion object {
        val keysThatRequireDndAccess = arrayOf(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP
        )
    }

    private val keyMapperImeHelper: KeyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

    override val invalidateTriggerErrors = merge(
            permissionAdapter.onPermissionsUpdate
    )

    override fun getTriggerErrors(keyMap: KeyMap): List<KeyMapTriggerError> {
        val trigger = keyMap.trigger
        val errors = mutableListOf<KeyMapTriggerError>()

        // can only detect volume button presses during a phone call with an input method service
        if (!keyMapperImeHelper.isCompatibleImeChosen() &&keyMap.requiresImeKeyEventForwarding()) {

            errors.add(KeyMapTriggerError.CANT_DETECT_IN_PHONE_CALL)
        }

        if (trigger.keys.any { it.keyCode in keysThatRequireDndAccess }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !permissionAdapter.isGranted(Permission.ACCESS_NOTIFICATION_POLICY)
            ) {
                errors.add(KeyMapTriggerError.DND_ACCESS_DENIED)
            }
        }

        if (trigger.screenOffTrigger
                && !permissionAdapter.isGranted(Permission.ROOT)
                && trigger.isDetectingWhenScreenOffAllowed()) {

            errors.add(KeyMapTriggerError.SCREEN_OFF_ROOT_DENIED)
        }

        return errors
    }
}

interface DisplayKeyMapUseCase : DisplaySimpleMappingUseCase {
    val invalidateTriggerErrors: Flow<Unit>
    fun getTriggerErrors(keyMap: KeyMap): List<KeyMapTriggerError>
}