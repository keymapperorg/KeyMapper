package io.github.sds100.keymapper.base.constraints

import android.content.pm.PackageManager
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.onSuccess
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

    override fun getError(constraint: Constraint): KMError? {
        when (constraint.data) {
            is ConstraintData.AppInForeground ->
                return getAppError(
                    constraint.data.packageName,
                    constraint.data.appName,
                    constraint.isNot,
                )

            is ConstraintData.AppPlayingMedia -> {
                if (!isPermissionGranted(Permission.NOTIFICATION_LISTENER)) {
                    return SystemError.PermissionDenied(Permission.NOTIFICATION_LISTENER)
                }

                return getAppError(
                    constraint.data.packageName,
                    constraint.data.appName,
                    constraint.isNot,
                )
            }

            ConstraintData.MediaPlaying -> {
                if (!isPermissionGranted(Permission.NOTIFICATION_LISTENER)) {
                    return SystemError.PermissionDenied(Permission.NOTIFICATION_LISTENER)
                }
            }

            is ConstraintData.BtDeviceConnected -> {
                if (!systemFeatureAdapter.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    return KMError.SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
                }

                if (!isPermissionGranted(Permission.FIND_NEARBY_DEVICES)) {
                    return SystemError.PermissionDenied(Permission.FIND_NEARBY_DEVICES)
                }
            }

            is ConstraintData.OrientationCustom,
            ConstraintData.OrientationLandscape,
            ConstraintData.OrientationPortrait,
                ->
                if (!isPermissionGranted(Permission.WRITE_SETTINGS)) {
                    return SystemError.PermissionDenied(Permission.WRITE_SETTINGS)
                }

            is ConstraintData.FlashlightOn -> {
                if (!flashLenses.contains(constraint.data.lens)) {
                    return when (constraint.data.lens) {
                        CameraLens.FRONT -> KMError.FrontFlashNotFound
                        CameraLens.BACK -> KMError.BackFlashNotFound
                    }
                }
            }

            is ConstraintData.WifiConnected -> {
                if (!isPermissionGranted(Permission.ACCESS_FINE_LOCATION)) {
                    return SystemError.PermissionDenied(Permission.ACCESS_FINE_LOCATION)
                }
            }

            is ConstraintData.ImeChosen -> {
                if (inputMethods.none { it.id == constraint.data.imeId }) {
                    return KMError.InputMethodNotFound(constraint.data.imeLabel)
                }
            }

            is ConstraintData.InPhoneCall,
            is ConstraintData.PhoneRinging,
            is ConstraintData.NotInPhoneCall,
                -> {
                if (!isPermissionGranted(Permission.READ_PHONE_STATE)) {
                    return SystemError.PermissionDenied(Permission.READ_PHONE_STATE)
                }
            }

            is ConstraintData.NotificationPosted -> {
                if (!isPermissionGranted(Permission.NOTIFICATION_LISTENER)) {
                    return SystemError.PermissionDenied(Permission.NOTIFICATION_LISTENER)
                }

                if (constraint.data is ConstraintData.NotificationPosted.FromApp) {
                    return getAppError(
                        constraint.data.packageName,
                        constraint.data.appName,
                        constraint.isNot,
                    )
                }
            }

            else -> Unit
        }

        return null
    }

    private fun getAppError(packageName: String, appName: String?, isNot: Boolean): KMError? {
        // If the constraint is negated then a missing or disabled app can never satisfy the
        // un-negated condition, so the negated condition is trivially true. There is nothing
        // to warn about.
        if (isNot) {
            return null
        }

        packageManager.isAppEnabled(packageName).onSuccess { isEnabled ->
            if (!isEnabled) {
                return KMError.AppDisabled(packageName, appName)
            }
        }

        if (!packageManager.isAppInstalled(packageName)) {
            return KMError.AppNotFound(packageName, appName)
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
    fun getError(constraint: Constraint): KMError?
}
