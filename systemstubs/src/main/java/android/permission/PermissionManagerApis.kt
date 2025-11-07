package android.permission

import android.os.Build

object PermissionManagerApis {
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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissionManager.grantRuntimePermission(
                packageName,
                permission,
                userId,
            )
        } else {
            permissionManager.grantRuntimePermission(
                packageName,
                permission,
                userId,
            )
        }
    }
}
