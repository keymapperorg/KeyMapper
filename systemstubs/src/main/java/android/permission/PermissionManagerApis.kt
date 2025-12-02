package android.permission

import android.content.pm.IPackageManager
import android.os.Build
import androidx.annotation.RequiresApi

object PermissionManagerApis {
    @RequiresApi(Build.VERSION_CODES.R)
    fun grantPermission(
        permissionManager: IPermissionManager,
        packageName: String,
        permission: String,
        deviceId: Int,
        userId: Int,
    ) {
        // In revisions of Android 14 the method to grant permissions changed
        // so try them all.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                permissionManager.grantRuntimePermission(
                    packageName,
                    permission,
                    deviceId,
                    userId,
                )
            } catch (_: NoSuchMethodError) {
                try {
                    permissionManager.grantRuntimePermission(
                        packageName,
                        permission,
                        "0",
                        userId,
                    )
                } catch (_: NoSuchMethodError) {
                    permissionManager.grantRuntimePermission(
                        packageName,
                        permission,
                        userId,
                    )
                }
            }
            // In Android 11 this method was moved from IPackageManager to IPermissionManager.
        } else {
            permissionManager.grantRuntimePermission(
                packageName,
                permission,
                userId,
            )
        }
    }

    // Used on Android 10 and older.
    fun grantPermission(
        packageManager: IPackageManager,
        packageName: String,
        permission: String,
        userId: Int,
    ) {
        packageManager.grantRuntimePermission(packageName, permission, userId)
    }
}
