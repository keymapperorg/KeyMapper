package io.github.sds100.keymapper.constraints

import android.content.pm.PackageManager
import android.os.Build
import io.github.sds100.keymapper.common.result.Error
import io.github.sds100.keymapper.common.result.onSuccess
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter

class LazyConstraintErrorSnapshot(
    private val packageManager: PackageManagerAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val systemFeatureAdapter: SystemFeatureAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val cameraAdapter: CameraAdapter,
) : ConstraintErrorSnapshot {

    private val inputMethods by lazy { inputMethodAdapter.inputMethods.value }
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

    override fun getError(constraint: Constraint): Error? {
        when (constraint) {
            is Constraint.AppInForeground -> return getAppError(constraint.packageName)
            is Constraint.AppNotInForeground -> return getAppError(constraint.packageName)

            is Constraint.AppPlayingMedia -> {
                if (!isPermissionGranted(Permission.NOTIFICATION_LISTENER)) {
                    return SystemError.PermissionDenied(Permission.NOTIFICATION_LISTENER)
                }

                return getAppError(constraint.packageName)
            }

            is Constraint.AppNotPlayingMedia -> {
                if (!isPermissionGranted(Permission.NOTIFICATION_LISTENER)) {
                    return SystemError.PermissionDenied(Permission.NOTIFICATION_LISTENER)
                }

                return getAppError(constraint.packageName)
            }

            Constraint.MediaPlaying, Constraint.NoMediaPlaying -> {
                if (!isPermissionGranted(Permission.NOTIFICATION_LISTENER)) {
                    return SystemError.PermissionDenied(Permission.NOTIFICATION_LISTENER)
                }
            }

            is Constraint.BtDeviceConnected,
            is Constraint.BtDeviceDisconnected,
            -> {
                if (!systemFeatureAdapter.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    return Error.SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
                }

                if (!isPermissionGranted(Permission.FIND_NEARBY_DEVICES)) {
                    return SystemError.PermissionDenied(Permission.FIND_NEARBY_DEVICES)
                }
            }

            is Constraint.OrientationCustom,
            Constraint.OrientationLandscape,
            Constraint.OrientationPortrait,
            ->
                if (!isPermissionGranted(Permission.WRITE_SETTINGS)) {
                    return SystemError.PermissionDenied(Permission.WRITE_SETTINGS)
                }

            is Constraint.FlashlightOn -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
                }

                if (!flashLenses.contains(constraint.lens)) {
                    return when (constraint.lens) {
                        CameraLens.FRONT -> Error.FrontFlashNotFound
                        CameraLens.BACK -> Error.BackFlashNotFound
                    }
                }
            }

            is Constraint.FlashlightOff -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
                }

                if (!flashLenses.contains(constraint.lens)) {
                    return when (constraint.lens) {
                        CameraLens.FRONT -> Error.FrontFlashNotFound
                        CameraLens.BACK -> Error.BackFlashNotFound
                    }
                }
            }

            is Constraint.WifiConnected, is Constraint.WifiDisconnected -> {
                if (!isPermissionGranted(Permission.ACCESS_FINE_LOCATION)) {
                    return SystemError.PermissionDenied(Permission.ACCESS_FINE_LOCATION)
                }
            }

            is Constraint.ImeChosen -> {
                if (inputMethods.none { it.id == constraint.imeId }) {
                    return Error.InputMethodNotFound(constraint.imeLabel)
                }
            }

            is Constraint.InPhoneCall, is Constraint.PhoneRinging, is Constraint.NotInPhoneCall -> {
                if (!isPermissionGranted(Permission.READ_PHONE_STATE)) {
                    return SystemError.PermissionDenied(Permission.READ_PHONE_STATE)
                }
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

interface ConstraintErrorSnapshot {
    fun getError(constraint: Constraint): Error?
}
