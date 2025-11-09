package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.system.notifications.NotificationController
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.camera.CameraFlashInfo
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.notifications.NotificationAdapter
import io.github.sds100.keymapper.system.notifications.NotificationModel
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.system.phone.PhoneAdapter
import io.github.sds100.keymapper.system.settings.SettingType
import io.github.sds100.keymapper.system.settings.SettingsAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

class CreateActionUseCaseImpl @Inject constructor(
    private val inputMethodAdapter: InputMethodAdapter,
    private val systemFeatureAdapter: SystemFeatureAdapter,
    private val cameraAdapter: CameraAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val phoneAdapter: PhoneAdapter,
    private val settingsAdapter: SettingsAdapter,
    private val notificationAdapter: NotificationAdapter,
) : CreateActionUseCase,
    IsActionSupportedUseCase by IsActionSupportedUseCaseImpl(
        systemFeatureAdapter,
        cameraAdapter,
        permissionAdapter,
    ) {
    override suspend fun getInputMethods(): List<ImeInfo> = inputMethodAdapter.inputMethods.first()

    override fun getFlashlightLenses(): Set<CameraLens> {
        return CameraLens.entries.filter { cameraAdapter.getFlashInfo(it) != null }.toSet()
    }

    override fun getFlashInfo(lens: CameraLens): CameraFlashInfo? {
        return cameraAdapter.getFlashInfo(lens)
    }

    override fun toggleFlashlight(lens: CameraLens, strength: Float) {
        cameraAdapter.toggleFlashlight(lens, strength)
    }

    override fun disableFlashlight() {
        cameraAdapter.disableFlashlight(CameraLens.FRONT)
        cameraAdapter.disableFlashlight(CameraLens.BACK)
    }

    override fun setFlashlightBrightness(lens: CameraLens, strength: Float) {
        cameraAdapter.enableFlashlight(lens, strength)
    }

    override fun isFlashlightEnabled(): Flow<Boolean> {
        return merge(
            cameraAdapter.isFlashlightOnFlow(CameraLens.FRONT),
            cameraAdapter.isFlashlightOnFlow(CameraLens.BACK),
        )
    }

    override fun requestPermission(permission: Permission) {
        permissionAdapter.request(permission)
    }

    override suspend fun testSms(number: String, message: String): KMResult<Unit> {
        if (!permissionAdapter.isGranted(Permission.SEND_SMS)) {
            return SystemError.PermissionDenied(Permission.SEND_SMS)
        }

        return phoneAdapter.sendSms(number, message)
    }

    override fun setSettingValue(
        settingType: SettingType,
        key: String,
        value: String,
    ): KMResult<Unit> {
        return settingsAdapter.setValue(settingType, key, value)
    }

    override fun getRequiredPermissionForSettingType(settingType: SettingType): Permission {
        return when (settingType) {
            SettingType.SYSTEM -> Permission.WRITE_SETTINGS
            SettingType.SECURE, SettingType.GLOBAL -> Permission.WRITE_SECURE_SETTINGS
        }
    }

    override fun isPermissionGrantedFlow(permission: Permission): Flow<Boolean> {
        return permissionAdapter.isGrantedFlow(permission)
    }

    override fun testCreateNotification(title: String, text: String, timeoutMs: Long?) {
        val notification = NotificationModel(
            // Use the same id for notifications created when testing so they overwrite each other
            id = 0,
            channel = NotificationController.CHANNEL_CUSTOM_NOTIFICATIONS,
            title = title,
            text = text,
            icon = R.drawable.ic_launcher_foreground,
            showOnLockscreen = true,
            onGoing = false,
            autoCancel = true,
            timeout = timeoutMs,
            bigTextStyle = true,
        )

        notificationAdapter.showNotification(notification)
    }
}

interface CreateActionUseCase : IsActionSupportedUseCase {
    suspend fun getInputMethods(): List<ImeInfo>

    fun isFlashlightEnabled(): Flow<Boolean>
    fun setFlashlightBrightness(lens: CameraLens, strength: Float)
    fun toggleFlashlight(lens: CameraLens, strength: Float)
    fun disableFlashlight()
    fun getFlashlightLenses(): Set<CameraLens>
    fun getFlashInfo(lens: CameraLens): CameraFlashInfo?

    fun requestPermission(permission: Permission)
    suspend fun testSms(number: String, message: String): KMResult<Unit>
    fun setSettingValue(settingType: SettingType, key: String, value: String): KMResult<Unit>
    fun getRequiredPermissionForSettingType(settingType: SettingType): Permission
    fun isPermissionGrantedFlow(permission: Permission): Flow<Boolean>
    fun testCreateNotification(title: String, text: String, timeoutMs: Long?)
}
