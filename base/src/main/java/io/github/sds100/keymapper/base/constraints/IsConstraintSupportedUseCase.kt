package io.github.sds100.keymapper.base.constraints

import android.content.pm.PackageManager
import android.os.Build
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter

class IsConstraintSupportedUseCaseImpl(
    private val systemFeatureAdapter: SystemFeatureAdapter,
    private val cameraAdapter: CameraAdapter,
) : IsConstraintSupportedUseCase {

    override fun isSupported(id: ConstraintId): KMError? {
        when (id) {
            ConstraintId.BT_DEVICE_CONNECTED -> {
                if (!systemFeatureAdapter.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    return KMError.SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
                }
            }

            ConstraintId.FLASHLIGHT_ON -> {
                if (cameraAdapter.getFlashInfo(CameraLens.BACK) == null &&
                    cameraAdapter.getFlashInfo(CameraLens.FRONT) == null
                ) {
                    return KMError.SystemFeatureNotSupported(PackageManager.FEATURE_CAMERA_FLASH)
                }
            }

            ConstraintId.HINGE_CLOSED,
            ConstraintId.HINGE_OPEN,
                -> {
                if (Build.VERSION.SDK_INT != 0 && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    return KMError.SdkVersionTooLow(Build.VERSION_CODES.R)
                }

                if (!systemFeatureAdapter.hasSystemFeature(
                        PackageManager.FEATURE_SENSOR_HINGE_ANGLE,
                    )
                ) {
                    return KMError.SystemFeatureNotSupported(
                        PackageManager.FEATURE_SENSOR_HINGE_ANGLE,
                    )
                }
            }

            else -> Unit
        }

        return null
    }
}

interface IsConstraintSupportedUseCase {
    fun isSupported(id: ConstraintId): KMError?
}
