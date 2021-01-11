package io.github.sds100.keymapper.data.repository

import io.github.sds100.keymapper.data.db.dao.DeviceInfoDao
import io.github.sds100.keymapper.data.model.DeviceInfo

/**
 * Created by sds100 on 26/01/2020.
 */
class DefaultDeviceInfoRepository internal constructor(private val deviceInfoDao: DeviceInfoDao) : DeviceInfoRepository {

    override suspend fun getAll(): List<DeviceInfo> {
        return deviceInfoDao.getAll()
    }

    override suspend fun insertDeviceInfo(vararg deviceInfo: DeviceInfo) {
        deviceInfoDao.insert(*deviceInfo)
    }

    override suspend fun getDeviceInfo(descriptor: String) {
        deviceInfoDao.getByDescriptor(descriptor)
    }

    override suspend fun deleteAll() {
        deviceInfoDao.deleteAll()
    }
}