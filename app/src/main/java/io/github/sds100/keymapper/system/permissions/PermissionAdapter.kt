package io.github.sds100.keymapper.system.permissions

import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 16/03/2021.
 */
interface PermissionAdapter {
    val onPermissionsUpdate: Flow<Unit>
    fun isGranted(permission: Permission): Boolean
    fun isGrantedFlow(permission: Permission): Flow<Boolean>
    fun request(permission: Permission)
}