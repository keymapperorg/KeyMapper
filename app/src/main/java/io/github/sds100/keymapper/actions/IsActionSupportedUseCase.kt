package io.github.sds100.keymapper.actions

import android.content.pm.PackageManager
import android.os.Build
import io.github.sds100.keymapper.common.result.Error
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

    override fun isSupported(id: ActionId): Error? {
        if (Build.VERSION.SDK_INT != 0) {
            val minApi = ActionUtils.getMinApi(id)

            if (Build.VERSION.SDK_INT < minApi) {
                return Error.SdkVersionTooLow(minApi)
            }

            val maxApi = ActionUtils.getMaxApi(id)

            if (Build.VERSION.SDK_INT > maxApi) {
                return Error.SdkVersionTooHigh(maxApi)
            }
        }

        ActionUtils.getRequiredSystemFeatures(id).forEach { feature ->
            if (!adapter.hasSystemFeature(feature)) {
                return Error.SystemFeatureNotSupported(feature)
            }
        }

        if (id == ActionId.ENABLE_FLASHLIGHT || id == ActionId.DISABLE_FLASHLIGHT || id == ActionId.TOGGLE_FLASHLIGHT) {
            if (cameraAdapter.getFlashInfo(CameraLens.BACK) == null &&
                cameraAdapter.getFlashInfo(CameraLens.FRONT) == null
            ) {
                return Error.SystemFeatureNotSupported(PackageManager.FEATURE_CAMERA_FLASH)
            }
        }

        if (id == ActionId.CHANGE_FLASHLIGHT_STRENGTH) {
            if (cameraAdapter.getFlashInfo(CameraLens.BACK)?.supportsVariableStrength != true &&
                cameraAdapter.getFlashInfo(CameraLens.FRONT)?.supportsVariableStrength != true
            ) {
                return Error.CameraVariableFlashlightStrengthUnsupported
            }
        }

        if (ActionUtils.getRequiredPermissions(id)
                .contains(Permission.ROOT) &&
            !permissionAdapter.isGranted(Permission.ROOT)
        ) {
            return SystemError.PermissionDenied(Permission.ROOT)
        }

        return null
    }
}

interface IsActionSupportedUseCase {
    fun isSupported(id: ActionId): Error?
}
