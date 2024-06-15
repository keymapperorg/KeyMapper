package io.github.sds100.keymapper.system.permissions

import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 16/03/2021.
 */
interface PermissionAdapter {
    val onPermissionsUpdate: Flow<Unit>
    fun isGranted(permission: Permission): Boolean
    fun isGrantedFlow(permission: Permission): Flow<Boolean>

    /**
     * Request a permission that requires the user to grant access.
     */
    fun request(permission: Permission)

    /**
     * Grant a permission automatically without requiring the user.
     * Requires root access or Shizuku.
     */
    fun grant(permissionName: String): Result<*>
}
