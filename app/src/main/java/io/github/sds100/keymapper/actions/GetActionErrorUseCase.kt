package io.github.sds100.keymapper.actions

import android.os.Build
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.shizuku.ShizukuAdapter
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
import io.github.sds100.keymapper.util.onSuccess
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
    private val cameraAdapter: CameraAdapter,
    private val soundsManager: SoundsManager,
    private val shizukuAdapter: ShizukuAdapter,
) : GetActionErrorUseCase {

    private val isActionSupported = IsActionSupportedUseCaseImpl(systemFeatureAdapter)
    private val keyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

    override val invalidateActionErrors = merge(
        inputMethodAdapter.chosenIme.drop(1).map { },
        // invalidate when the input methods change
        inputMethodAdapter.inputMethods.drop(1).map { },
        permissionAdapter.onPermissionsUpdate,
        soundsManager.soundFiles.drop(1).map { },
        shizukuAdapter.isStarted.drop(1).map { },
        shizukuAdapter.isInstalled.drop(1).map { },
        packageManager.onPackagesChanged,
    )

    override fun getError(action: ActionData): Error? {
        if (action.canUseShizukuToPerform() && shizukuAdapter.isInstalled.value) {
            if (!(action.canUseImeToPerform() && keyMapperImeHelper.isCompatibleImeChosen())) {
                when {
                    !shizukuAdapter.isStarted.value ->
                        return Error.ShizukuNotStarted

                    !permissionAdapter.isGranted(Permission.SHIZUKU) ->
                        return Error.PermissionDenied(Permission.SHIZUKU)
                }
            }
        } else if (action.canUseImeToPerform()) {
            if (!keyMapperImeHelper.isCompatibleImeEnabled()) {
                return Error.NoCompatibleImeEnabled
            }

            if (!keyMapperImeHelper.isCompatibleImeChosen()) {
                return Error.NoCompatibleImeChosen
            }
        }

        isActionSupported.invoke(action.id)?.let {
            return it
        }

        ActionUtils.getRequiredPermissions(action.id).forEach { permission ->
            if (!permissionAdapter.isGranted(permission)) {
                return Error.PermissionDenied(permission)
            }
        }

        when (action) {
            is ActionData.App -> {
                return getAppError(action.packageName)
            }

            is ActionData.AppShortcut -> {
                action.packageName ?: return null

                return getAppError(action.packageName)
            }

            is ActionData.InputKeyEvent ->
                if (
                    action.useShell && !permissionAdapter.isGranted(Permission.ROOT)
                ) {
                    return Error.PermissionDenied(Permission.ROOT)
                }

            is ActionData.TapScreen ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    return Error.SdkVersionTooLow(Build.VERSION_CODES.N)
                }

            is ActionData.Sound -> {
                soundsManager.getSound(action.soundUid).onFailure { error ->
                    return error
                }
            }

            is ActionData.VoiceAssistant -> {
                if (!packageManager.isVoiceAssistantInstalled()) {
                    return Error.NoVoiceAssistant
                }
            }

            is ActionData.Flashlight ->
                if (!cameraAdapter.hasFlashFacing(action.lens)) {
                    return when (action.lens) {
                        CameraLens.FRONT -> Error.FrontFlashNotFound
                        CameraLens.BACK -> Error.BackFlashNotFound
                    }
                }

            is ActionData.SwitchKeyboard ->
                inputMethodAdapter.getInfoById(action.imeId).onFailure {
                    return it
                }

            else -> Unit
        }

        return null
    }

    private fun getAppError(packageName: String): Error? {
        packageManager.isAppEnabled(packageName).onSuccess { isEnabled ->
            if (!isEnabled) {
                return Error.AppDisabled(packageName)
            }
        }

        if (!packageManager.isAppInstalled(packageName)) {
            return Error.AppNotFound(packageName)
        }

        return null
    }
}

interface GetActionErrorUseCase {
    val invalidateActionErrors: Flow<Unit>
    fun getError(action: ActionData): Error?
}
