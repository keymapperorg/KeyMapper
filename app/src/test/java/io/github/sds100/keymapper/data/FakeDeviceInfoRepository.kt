package io.github.sds100.keymapper.data

import io.github.sds100.keymapper.data.model.DeviceInfo

/**
 * Created by sds100 on 21/05/20.
 */
class FakeDeviceInfoRepository : DeviceInfoRepository {
    private val mDeviceInfoList = mutableMapOf<String, String>()

    override suspend fun getAll() = mDeviceInfoList.map { DeviceInfo(it.key, it.value) }
    override suspend fun createDeviceInfo(deviceInfo: DeviceInfo) {
        mDeviceInfoList[deviceInfo.descriptor] = deviceInfo.name
    }

    override suspend fun getDeviceInfo(descriptor: String) {
        TODO("Not yet implemented")
    }
}