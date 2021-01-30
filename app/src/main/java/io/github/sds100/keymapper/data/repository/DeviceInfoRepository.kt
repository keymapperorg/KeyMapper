package io.github.sds100.keymapper.data.repository

import io.github.sds100.keymapper.data.model.DeviceInfo

/**
 * Created by sds100 on 17/05/2020.
 */
interface DeviceInfoRepository {
    suspend fun getAll(): List<DeviceInfo>
    fun insertDeviceInfo(vararg deviceInfo: DeviceInfo)
    fun getDeviceInfo(descriptor: String)
    fun deleteAll()
}