package io.github.sds100.keymapper.mappings.keymaps

import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapTrigger
import io.github.sds100.keymapper.mappings.DisplaySimpleMappingUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapTriggerError
import io.github.sds100.keymapper.system.permissions.Permission

/**
 * Created by sds100 on 04/04/2021.
 */

class DisplayKeyMapUseCaseImpl(
    private val permissionAdapter: PermissionAdapter,
    displaySimpleMappingUseCase: DisplaySimpleMappingUseCase
) : DisplayKeyMapUseCase, DisplaySimpleMappingUseCase by displaySimpleMappingUseCase {
    private companion object {
        val keysThatRequireDndAccess = arrayOf(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP
        )
    }

    override fun getTriggerErrors(trigger: KeyMapTrigger): List<KeyMapTriggerError> {
        val errors = mutableListOf<KeyMapTriggerError>()

        if (trigger.keys.any { it.keyCode in keysThatRequireDndAccess }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !permissionAdapter.isGranted(Permission.ACCESS_NOTIFICATION_POLICY)
            ) {
                errors.add(KeyMapTriggerError.DND_ACCESS_DENIED)
            }
        }

        if (trigger.screenOffTrigger && !permissionAdapter.isGranted(Permission.ROOT) && trigger.isDetectingWhenScreenOffAllowed()){
            errors.add(KeyMapTriggerError.SCREEN_OFF_ROOT_DENIED)
        }

        return errors
    }
}

interface DisplayKeyMapUseCase : DisplaySimpleMappingUseCase {
    fun getTriggerErrors(trigger: KeyMapTrigger): List<KeyMapTriggerError>
}