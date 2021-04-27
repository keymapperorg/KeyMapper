package io.github.sds100.keymapper.actions

import android.os.Build
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * Created by sds100 on 15/02/2021.
 */

class GetActionErrorUseCaseImpl(
    private val packageManager: PackageManagerAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val systemFeatureAdapter: SystemFeatureAdapter,
    private val cameraAdapter: CameraAdapter
) : GetActionErrorUseCase {

    private val isSystemActionSupported = IsSystemActionSupportedUseCaseImpl(systemFeatureAdapter)
    private val keyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

    override val invalidateErrors = merge(
        inputMethodAdapter.chosenIme.drop(1).map { },
        permissionAdapter.onPermissionsUpdate
    )

    override fun getError(action: ActionData): Error? {
        if (action.requiresImeToPerform()) {
            if (!keyMapperImeHelper.isCompatibleImeEnabled()) {
                return Error.NoCompatibleImeEnabled
            }

            if (!keyMapperImeHelper.isCompatibleImeChosen()) {
                return Error.NoCompatibleImeChosen
            }
        }

        when (action) {
            is OpenAppAction -> {
                return getAppError(action.packageName)
            }

            is OpenAppShortcutAction -> {
                action.packageName ?: return null

                return getAppError(action.packageName)
            }

            is KeyEventAction ->
                if (
                    action.useShell && !permissionAdapter.isGranted(Permission.ROOT)
                ) {
                   return Error.PermissionDenied(Permission.ROOT)
                }

            is TapCoordinateAction ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    return Error.SdkVersionTooLow(Build.VERSION_CODES.N)
                }

            is PhoneCallAction ->
                if (!permissionAdapter.isGranted(Permission.CALL_PHONE)) {
                    return Error.PermissionDenied(Permission.CALL_PHONE)
                }

            is SystemAction -> return action.getError()
        }

        return null
    }

    private fun getAppError(packageName: String): Error? {
        if (!packageManager.isAppEnabled(packageName)) {
            return Error.AppDisabled(packageName)
        }

        if (!packageManager.isAppInstalled(packageName)) {
            return Error.AppNotFound(packageName)
        }

        return null
    }

    private fun SystemAction.getError(): Error? {
        isSystemActionSupported.invoke(this.id)?.let {
            return it
        }

        SystemActionUtils.getRequiredPermissions(this.id).forEach { permission ->
            if (!permissionAdapter.isGranted(permission)) {
                return Error.PermissionDenied(permission)
            }
        }

        when {
            id == SystemActionId.OPEN_VOICE_ASSISTANT -> if (!packageManager.isVoiceAssistantInstalled()) {
                return Error.NoVoiceAssistant
            }

            this is FlashlightSystemAction ->
                if (!cameraAdapter.hasFlashFacing(this.lens)) {
                    return when (lens) {
                        CameraLens.FRONT -> Error.FrontFlashNotFound
                        CameraLens.BACK -> Error.BackFlashNotFound
                    }
                }

            this is SwitchKeyboardSystemAction ->
                inputMethodAdapter.getInfoById(this.imeId).onFailure {
                    return it
                }
        }

        return null
    }
}

interface GetActionErrorUseCase {
    val invalidateErrors: Flow<Unit>
    fun getError(action: ActionData): Error?
}