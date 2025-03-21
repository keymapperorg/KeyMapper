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

class LazyActionErrorSnapshot(
    private val packageManager: PackageManagerAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val permissionAdapter: PermissionAdapter,
    systemFeatureAdapter: SystemFeatureAdapter,
    cameraAdapter: CameraAdapter,
    private val soundsManager: SoundsManager,
    shizukuAdapter: ShizukuAdapter,
) : ActionErrorSnapshot {
    private val keyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

    private val isCompatibleImeEnabled by lazy { keyMapperImeHelper.isCompatibleImeEnabled() }
    private val isCompatibleImeChosen by lazy { keyMapperImeHelper.isCompatibleImeChosen() }
    private val isShizukuInstalled by lazy { shizukuAdapter.isInstalled.value }
    private val isShizukuStarted by lazy { shizukuAdapter.isStarted.value }
    private val isVoiceAssistantInstalled by lazy { packageManager.isVoiceAssistantInstalled() }
    private val grantedPermissions: MutableMap<Permission, Boolean> = mutableMapOf()
    private val flashLenses by lazy {
        buildSet {
            if (cameraAdapter.hasFlashFacing(CameraLens.FRONT)) {
                add(CameraLens.FRONT)
            }

            if (cameraAdapter.hasFlashFacing(CameraLens.BACK)) {
                add(CameraLens.BACK)
            }
        }
    }

    private val systemFeatures by lazy { systemFeatureAdapter.getSystemFeatures() }

    override fun getError(action: ActionData): Error? {
        if (Build.VERSION.SDK_INT != 0) {
            val minApi = ActionUtils.getMinApi(action.id)

            if (Build.VERSION.SDK_INT < minApi) {
                return Error.SdkVersionTooLow(minApi)
            }

            val maxApi = ActionUtils.getMaxApi(action.id)

            if (Build.VERSION.SDK_INT > maxApi) {
                return Error.SdkVersionTooHigh(maxApi)
            }
        }

        ActionUtils.getRequiredSystemFeatures(action.id).forEach { feature ->
            if (!systemFeatures.contains(feature)) {
                return Error.SystemFeatureNotSupported(feature)
            }
        }

        if (action.canUseShizukuToPerform() && isShizukuInstalled) {
            if (!(action.canUseImeToPerform() && isCompatibleImeChosen)) {
                when {
                    !isShizukuStarted -> return Error.ShizukuNotStarted

                    !isPermissionGranted(Permission.SHIZUKU) -> return Error.PermissionDenied(
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
                    action.useShell && !isPermissionGranted(Permission.ROOT)
                ) {
                    return Error.PermissionDenied(Permission.ROOT)
                }

            is ActionData.Sound -> {
                soundsManager.getSound(action.soundUid).onFailure { error ->
                    return error
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
