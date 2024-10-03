package io.github.sds100.keymapper.mappings.keymaps

import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.DisplaySimpleMappingUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerError
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * Created by sds100 on 04/04/2021.
 */

class DisplayKeyMapUseCaseImpl(
    private val permissionAdapter: PermissionAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    displaySimpleMappingUseCase: DisplaySimpleMappingUseCase,
    private val preferenceRepository: PreferenceRepository,
) : DisplayKeyMapUseCase,
    DisplaySimpleMappingUseCase by displaySimpleMappingUseCase {
    private companion object {
        val keysThatRequireDndAccess = arrayOf(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
        )
    }

    private val keyMapperImeHelper: KeyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

    override val invalidateTriggerErrors = merge(
        permissionAdapter.onPermissionsUpdate,
        preferenceRepository.get(Keys.neverShowDndError).map { }.drop(1),
    )

    override suspend fun getTriggerErrors(keyMap: KeyMap): List<TriggerError> {
        val trigger = keyMap.trigger
        val errors = mutableListOf<TriggerError>()

        // can only detect volume button presses during a phone call with an input method service
        if (!keyMapperImeHelper.isCompatibleImeChosen() && keyMap.requiresImeKeyEventForwarding()) {
            errors.add(TriggerError.CANT_DETECT_IN_PHONE_CALL)
        }

        if (trigger.keys.any { it.keyCode in keysThatRequireDndAccess }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !permissionAdapter.isGranted(Permission.ACCESS_NOTIFICATION_POLICY)
            ) {
                errors.add(TriggerError.DND_ACCESS_DENIED)
            }
        }

        if (trigger.screenOffTrigger &&
            !permissionAdapter.isGranted(Permission.ROOT) &&
            trigger.isDetectingWhenScreenOffAllowed()
        ) {
            errors.add(TriggerError.SCREEN_OFF_ROOT_DENIED)
        }

        return errors
    }
}

interface DisplayKeyMapUseCase : DisplaySimpleMappingUseCase {
    val invalidateTriggerErrors: Flow<Unit>
    suspend fun getTriggerErrors(keyMap: KeyMap): List<TriggerError>
}
