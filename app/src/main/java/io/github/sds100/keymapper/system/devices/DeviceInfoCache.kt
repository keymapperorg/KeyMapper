package io.github.sds100.keymapper.system.devices

import io.github.sds100.keymapper.util.Result

/**
 * Created by sds100 on 17/05/2020.
 */

//TODO delete because device names should be saved with actions and triggers
interface DeviceInfoCache {
    suspend fun getAll(): List<DeviceInfoEntity>
    fun insertDeviceInfo(vararg deviceInfo: DeviceInfoEntity)
    suspend fun getDeviceName(descriptor: String): Result<String>
    fun deleteAll()
}