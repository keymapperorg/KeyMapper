package io.github.sds100.keymapper.base.actions

import android.annotation.SuppressLint
import io.github.sds100.keymapper.base.actions.sound.SoundsManager
import io.github.sds100.keymapper.base.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.base.system.inputmethod.SwitchImeInterface
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.sysbridge.utils.SystemBridgeError
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.system.ringtones.RingtoneAdapter

class LazyActionErrorSnapshot(
    private val packageManager: PackageManagerAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val switchImeInterface: SwitchImeInterface,
    private val permissionAdapter: PermissionAdapter,
    systemFeatureAdapter: SystemFeatureAdapter,
    cameraAdapter: CameraAdapter,
    private val soundsManager: SoundsManager,
    private val ringtoneAdapter: RingtoneAdapter,
    private val buildConfigProvider: BuildConfigProvider,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val preferenceRepository: PreferenceRepository,
) : ActionErrorSnapshot,
    IsActionSupportedUseCase by IsActionSupportedUseCaseImpl(
        systemFeatureAdapter,
        cameraAdapter,
        permissionAdapter,
    ) {
    private val keyMapperImeHelper =
        KeyMapperImeHelper(switchImeInterface, inputMethodAdapter, buildConfigProvider.packageName)

    private val isCompatibleImeEnabled by lazy { keyMapperImeHelper.isCompatibleImeEnabled() }
    private val isCompatibleImeChosen by lazy { keyMapperImeHelper.isCompatibleImeChosen() }
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

    private val isSystemBridgeConnected: Boolean by lazy {
        if (buildConfigProvider.sdkInt >= Constants.SYSTEM_BRIDGE_MIN_API) {
            @SuppressLint("NewApi")
            systemBridgeConnectionManager.connectionState.value is SystemBridgeConnectionState.Connected
        } else {
            false
        }
    }

    private val keyEventActionsUseSystemBridge: Boolean by lazy {
        if (buildConfigProvider.sdkInt >= Constants.SYSTEM_BRIDGE_MIN_API) {
            preferenceRepository.get(Keys.keyEventActionsUseSystemBridge).firstBlocking() ?: false
        } else {
            false
        }
    }

    override fun getErrors(actions: List<ActionData>): Map<ActionData, KMError?> {
        // Fixes #797 and #1719
        // Store which input method would be selected if the actions run successfully.
        // Errors should not be thrown for actions that will be fixed by previous ones.
        var currentImeFromActions: ImeInfo? = null

        val errorMap = mutableMapOf<ActionData, KMError?>()

        for (action in actions) {
            if (action is ActionData.SwitchKeyboard) {
                currentImeFromActions = inputMethodAdapter.getInfoById(action.imeId).valueOrNull()
            }

            var error = getError(action)

            val isImeNotChosenError =
                error == KMError.NoCompatibleImeChosen ||
                    (
                        error is KMError.KeyEventActionError &&
                            error.baseError == KMError.NoCompatibleImeChosen
                        )

            if (isImeNotChosenError && currentImeFromActions != null) {
                val isCurrentImeCompatible =
                    KeyMapperImeHelper.isKeyMapperInputMethod(
                        currentImeFromActions.packageName,
                        buildConfigProvider.packageName,
                    )

                if (isCurrentImeCompatible) {
                    error = null
                }
            }

            errorMap[action] = error
        }

        return errorMap
    }

    override fun getError(action: ActionData): KMError? {
        val isSupportedError = isSupported(action.id)

        if (isSupportedError != null) {
            return isSupportedError
        }

        if (buildConfigProvider.sdkInt >= Constants.SYSTEM_BRIDGE_MIN_API &&
            action is ActionData.InputKeyEvent &&
            keyEventActionsUseSystemBridge
        ) {
            if (!isSystemBridgeConnected) {
                return KMError.KeyEventActionError(SystemBridgeError.Disconnected)
            }
        } else if (action.canUseImeToPerform()) {
            if (!isCompatibleImeEnabled) {
                if (action is ActionData.InputKeyEvent) {
                    return KMError.KeyEventActionError(KMError.NoCompatibleImeEnabled)
                } else {
                    return KMError.NoCompatibleImeEnabled
                }
            }

            if (!isCompatibleImeChosen) {
                if (action is ActionData.InputKeyEvent) {
                    return KMError.KeyEventActionError(KMError.NoCompatibleImeChosen)
                } else {
                    return KMError.NoCompatibleImeChosen
                }
            }
        }

        @SuppressLint("NewApi")
        if (buildConfigProvider.sdkInt >= Constants.SYSTEM_BRIDGE_MIN_API &&
            ActionUtils.isSystemBridgeRequired(action.id) &&
            !isSystemBridgeConnected
        ) {
            return SystemBridgeError.Disconnected
        }

        for (permission in ActionUtils.getRequiredPermissions(action.id)) {
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

            is ActionData.Sound.SoundFile -> {
                soundsManager.getSound(action.soundUid).onFailure { error ->
                    return error
                }
            }

            is ActionData.Sound.Ringtone -> {
                if (!ringtoneAdapter.exists(action.uri)) {
                    return KMError.CantFindSoundFile
                }
            }

            is ActionData.VoiceAssistant -> {
                if (!isVoiceAssistantInstalled) {
                    return KMError.NoVoiceAssistant
                }
            }

            is ActionData.Flashlight ->
                if (!flashLenses.contains(action.lens)) {
                    return when (action.lens) {
                        CameraLens.FRONT -> KMError.FrontFlashNotFound
                        CameraLens.BACK -> KMError.BackFlashNotFound
                    }
                }

            is ActionData.SwitchKeyboard ->
                inputMethodAdapter.getInfoById(action.imeId).onFailure {
                    return it
                }

            is ActionData.ShellCommand -> {
                return when (action.executionMode) {
                    ShellExecutionMode.ROOT -> {
                        if (!isPermissionGranted(Permission.ROOT)) {
                            SystemError.PermissionDenied(Permission.ROOT)
                        } else {
                            null
                        }
                    }

                    ShellExecutionMode.ADB -> {
                        if (!isSystemBridgeConnected) {
                            SystemBridgeError.Disconnected
                        } else {
                            null
                        }
                    }

                    ShellExecutionMode.STANDARD -> null
                }
            }

            else -> {}
        }

        return null
    }

    private fun getAppError(packageName: String): KMError? {
        packageManager.isAppEnabled(packageName).onSuccess { isEnabled ->
            if (!isEnabled) {
                return KMError.AppDisabled(packageName)
            }
        }

        if (!packageManager.isAppInstalled(packageName)) {
            return KMError.AppNotFound(packageName)
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
    fun getError(action: ActionData): KMError?
    fun getErrors(actions: List<ActionData>): Map<ActionData, KMError?>
}
