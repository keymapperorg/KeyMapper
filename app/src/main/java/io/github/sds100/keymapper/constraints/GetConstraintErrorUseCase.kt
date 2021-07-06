package io.github.sds100.keymapper.constraints

import android.content.pm.PackageManager
import android.os.Build
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.onSuccess
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 17/04/2021.
 */

class GetConstraintErrorUseCaseImpl(
    private val packageManager: PackageManagerAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val systemFeatureAdapter: SystemFeatureAdapter,
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

            is Constraint.BtDeviceConnected,
            is Constraint.BtDeviceDisconnected ->
                if (!systemFeatureAdapter.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    return Error.SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
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