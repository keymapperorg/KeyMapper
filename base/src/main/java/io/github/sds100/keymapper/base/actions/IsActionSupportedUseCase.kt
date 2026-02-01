package io.github.sds100.keymapper.base.actions

import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter

class IsActionSupportedUseCaseImpl(
    private val adapter: SystemFeatureAdapter,
    private val cameraAdapter: CameraAdapter,
    private val permissionAdapter: PermissionAdapter,
) : IsActionSupportedUseCase {

    override fun isSupported(id: ActionId): KMError? {
        if (Build.VERSION.SDK_INT != 0) {
            val minApi = ActionUtils.getMinApi(id)

            if (Build.VERSION.SDK_INT < minApi) {
                return KMError.SdkVersionTooLow(minApi)
            }

            val maxApi = ActionUtils.getMaxApi(id)

            if (Build.VERSION.SDK_INT > maxApi) {
                return KMError.SdkVersionTooHigh(maxApi)
            }
        }

        ActionUtils.getRequiredSystemFeatures(id).forEach { feature ->
            if (!adapter.hasSystemFeature(feature)) {
                return KMError.SystemFeatureNotSupported(feature)
            }
        }

        if (id == ActionId.ENABLE_FLASHLIGHT ||
            id == ActionId.DISABLE_FLASHLIGHT ||
            id == ActionId.TOGGLE_FLASHLIGHT
        ) {
            if (cameraAdapter.getFlashInfo(CameraLens.BACK) == null &&
                cameraAdapter.getFlashInfo(CameraLens.FRONT) == null
            ) {
                return KMError.SystemFeatureNotSupported(PackageManager.FEATURE_CAMERA_FLASH)
            }
        }

        if (id == ActionId.CHANGE_FLASHLIGHT_STRENGTH) {
            if (cameraAdapter.getFlashInfo(CameraLens.BACK)?.supportsVariableStrength != true &&
                cameraAdapter.getFlashInfo(CameraLens.FRONT)?.supportsVariableStrength != true
            ) {
                return KMError.CameraVariableFlashlightStrengthUnsupported
            }
        }

        if (id == ActionId.TOGGLE_NIGHT_SHIFT ||
            id == ActionId.ENABLE_NIGHT_SHIFT ||
            id == ActionId.DISABLE_NIGHT_SHIFT
        ) {
            // See https://cs.android.com/android/platform/superproject/+/android-latest-release:frameworks/base/core/java/android/hardware/display/ColorDisplayManager.java;l=498;drc=787314ed22d859e510163327dd6c58b215c2f7f9
            val res = Resources.getSystem()
            val resId = res.getIdentifier("config_nightDisplayAvailable", "bool", "android")
            if (resId == 0 || !res.getBoolean(resId)) {
                return KMError.NightDisplayNotSupported
            }
        }

        if (ActionUtils.getRequiredPermissions(id).contains(Permission.ROOT) &&
            !permissionAdapter.isGranted(Permission.ROOT)
        ) {
            return SystemError.PermissionDenied(Permission.ROOT)
        }

        return null
    }
}

interface IsActionSupportedUseCase {
    fun isSupported(id: ActionId): KMError?
}
