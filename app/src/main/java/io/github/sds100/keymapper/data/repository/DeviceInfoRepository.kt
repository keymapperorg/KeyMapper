package io.github.sds100.keymapper.data.repository

import io.github.sds100.keymapper.data.model.DeviceInfo

/**
 * Created by sds100 on 17/05/2020.
 */
interface DeviceInfoRepository {
    suspend fun getAll(): List<DeviceInfo>
    suspend fun insertDeviceInfo(vararg deviceInfo: DeviceInfo)
    suspend fun getDeviceInfo(descriptor: String)
    suspend fun deleteAll()
}