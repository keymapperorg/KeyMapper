package io.github.sds100.keymapper.constraints

import android.content.pm.PackageManager
import android.os.Build
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.onSuccess
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Created by sds100 on 17/04/2021.
 */

class GetConstraintErrorUseCaseImpl @Inject constructor(
    private val packageManager: PackageManagerAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val systemFeatureAdapter: SystemFeatureAdapter,
    private val inputMethodAdapter: InputMethodAdapter
) : GetConstraintErrorUseCase {

    override val invalidateConstraintErrors: Flow<Unit> = permissionAdapter.onPermissionsUpdate

    override fun getConstraintError(constraint: Constraint): Error? {

        when (constraint) {
            is Constraint.AppInForeground -> return getAppError(constraint.packageName)
            is Constraint.AppNotInForeground -> return getAppError(constraint.packageName)

            is Constraint.AppPlayingMedia -> {
                if (!permissionAdapter.isGranted(Permission.NOTIFICATION_LISTENER)) {
                    return Error.PermissionDenied(Permission.NOTIFICATION_LISTENER)
                }

                return getAppError(constraint.packageName)
            }

            is Constraint.AppNotPlayingMedia -> {
                if (!permissionAdapter.isGranted(Permission.NOTIFICATION_LISTENER)) {
                    return Error.PermissionDenied(Permission.NOTIFICATION_LISTENER)
                }

                return getAppError(constraint.packageName)
            }

            Constraint.MediaPlaying, Constraint.NoMediaPlaying -> {
                if (!permissionAdapter.isGranted(Permission.NOTIFICATION_LISTENER)) {
                    return Error.PermissionDenied(Permission.NOTIFICATION_LISTENER)
                }
            }

            is Constraint.BtDeviceConnected,
            is Constraint.BtDeviceDisconnected -> {
                if (!systemFeatureAdapter.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    return Error.SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
                }

                if (!permissionAdapter.isGranted(Permission.FIND_NEARBY_DEVICES)) {
                    return Error.PermissionDenied(Permission.FIND_NEARBY_DEVICES)
                }
            }

            is Constraint.OrientationCustom,
            Constraint.OrientationLandscape,
            Constraint.OrientationPortrait ->
                if (!permissionAdapter.isGranted(Permission.WRITE_SETTINGS)) {
                    return Error.PermissionDenied(Permission.WRITE_SETTINGS)
                }

            Constraint.ScreenOff,
            Constraint.ScreenOn -> {
                if (!permissionAdapter.isGranted(Permission.ROOT)) {
                    return Error.PermissionDenied(Permission.ROOT)
                }
            }

            is Constraint.FlashlightOn, is Constraint.FlashlightOff -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
                }
            }

            is Constraint.WifiConnected, is Constraint.WifiDisconnected -> {
                if (!permissionAdapter.isGranted(Permission.ACCESS_FINE_LOCATION)) {
                    return Error.PermissionDenied(Permission.ACCESS_FINE_LOCATION)
                }
            }

            is Constraint.ImeChosen -> {
                if (inputMethodAdapter.inputMethods.value.none { it.id == constraint.imeId }) {
                    return Error.InputMethodNotFound(constraint.imeLabel)
                }
            }

            is Constraint.InPhoneCall, is Constraint.PhoneRinging, is Constraint.NotInPhoneCall -> {
                if (!permissionAdapter.isGranted(Permission.READ_PHONE_STATE)) {
                    return Error.PermissionDenied(Permission.READ_PHONE_STATE)
                }
            }
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

interface GetConstraintErrorUseCase {
    val invalidateConstraintErrors: Flow<Unit>

    fun getConstraintError(constraint: Constraint): Error?
}