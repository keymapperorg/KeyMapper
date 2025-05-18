package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.actions.sound.SoundsManager
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.base.shizuku.ShizukuAdapter
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.system.ringtones.RingtoneAdapter

class LazyActionErrorSnapshot(
    private val packageManager: PackageManagerAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val permissionAdapter: PermissionAdapter,
    systemFeatureAdapter: SystemFeatureAdapter,
    cameraAdapter: CameraAdapter,
    private val soundsManager: SoundsManager,
    shizukuAdapter: ShizukuAdapter,
    private val ringtoneAdapter: RingtoneAdapter,
) : ActionErrorSnapshot,
    IsActionSupportedUseCase by IsActionSupportedUseCaseImpl(
        systemFeatureAdapter,
        cameraAdapter,
        permissionAdapter,
    ) {
    private val keyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

    private val isCompatibleImeEnabled by lazy { keyMapperImeHelper.isCompatibleImeEnabled() }
    private val isCompatibleImeChosen by lazy { keyMapperImeHelper.isCompatibleImeChosen() }
    private val isShizukuInstalled by lazy { shizukuAdapter.isInstalled.value }
    private val isShizukuStarted by lazy { shizukuAdapter.isStarted.value }
    private val isVoiceAssistantInstalled by lazy { packageManager.isVoiceAssistantInstalled() }
    private val grantedPermissions: MutableMap<Permission, Boolean> = mutableMapOf()
    private val flashLenses by lazy {
        buildSet {
            if (cameraAdapter.getFlashInfo(CameraLens.FRONT) != null) {
                add(CameraLens.FRONT)
            }

            if (cameraAdapter.getFlashInfo(CameraLens.BACK) != null) {
                add(CameraLens.BACK)
            }
        }
    }

    override fun getError(action: ActionData): Error? {
        val isSupportedError = isSupported(action.id)

        if (isSupportedError != null) {
            return isSupportedError
        }

        if (action.canUseShizukuToPerform() && isShizukuInstalled) {
            if (!(action.canUseImeToPerform() && isCompatibleImeChosen)) {
                when {
                    !isShizukuStarted -> return Error.ShizukuNotStarted

                    !isPermissionGranted(Permission.SHIZUKU) -> return SystemError.PermissionDenied(
                        Permission.SHIZUKU,
                    )
                }
            }
        } else if (action.canUseImeToPerform()) {
            if (!isCompatibleImeEnabled) {
                return Error.NoCompatibleImeEnabled
            }

            if (!isCompatibleImeChosen) {
                return Error.NoCompatibleImeChosen
            }
        }

        ActionUtils.getRequiredPermissions(action.id).forEach { permission ->
            if (!isPermissionGranted(permission)) {
                return SystemError.PermissionDenied(permission)
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
                    action.useShell && !isPermissionGranted(Permission.ROOT)
                ) {
                    return SystemError.PermissionDenied(Permission.ROOT)
                }

            is ActionData.Sound.SoundFile -> {
                soundsManager.getSound(action.soundUid).onFailure { error ->
                    return error
                }
            }

            is ActionData.Sound.Ringtone -> {
                if (!ringtoneAdapter.exists(action.uri)) {
                    return Error.CantFindSoundFile
                }
            }

            is ActionData.VoiceAssistant -> {
                if (!isVoiceAssistantInstalled) {
                    return Error.NoVoiceAssistant
                }
            }

            is ActionData.Flashlight ->
                if (!flashLenses.contains(action.lens)) {
                    return when (action.lens) {
                        CameraLens.FRONT -> Error.FrontFlashNotFound
                        CameraLens.BACK -> Error.BackFlashNotFound
                    }
                }

            is ActionData.SwitchKeyboard ->
                inputMethodAdapter.getInfoById(action.imeId).onFailure {
                    return it
                }

            else -> {}
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

    private fun isPermissionGranted(permission: Permission): Boolean {
        if (grantedPermissions.contains(permission)) {
            return grantedPermissions[permission]!!
        } else {
            val isGranted = permissionAdapter.isGranted(permission)
            grantedPermissions[permission] = isGranted
            return isGranted
        }
    }
}

interface ActionErrorSnapshot {
    fun getError(action: ActionData): Error?
}
