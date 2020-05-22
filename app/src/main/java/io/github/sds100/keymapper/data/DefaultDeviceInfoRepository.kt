package com.example.architecturetest.data

import io.github.sds100.keymapper.data.db.dao.DeviceInfoDao
import io.github.sds100.keymapper.data.DeviceInfoRepository
import io.github.sds100.keymapper.data.model.DeviceInfo

/**
 * Created by sds100 on 26/01/2020.
 */
class DefaultDeviceInfoRepository internal constructor(private val mDeviceInfoDao: DeviceInfoDao) : DeviceInfoRepository {

    companion object {
        @Volatile
        private var instance: DefaultDeviceInfoRepository? = null

        fun getInstance(deviceInfoDao: DeviceInfoDao) =
            instance ?: synchronized(this) {
                instance ?: DefaultDeviceInfoRepository(deviceInfoDao).also { instance = it }
            }
    }

    override suspend fun getAll(): List<DeviceInfo> {
        return mDeviceInfoDao.getAll()
    }

    override suspend fun createDeviceInfo(deviceInfo: DeviceInfo) {
        mDeviceInfoDao.insert(deviceInfo)
    }

    override suspend fun getDeviceInfo(descriptor: String) {
        mDeviceInfoDao.getByDescriptor(descriptor)
    }

    override suspend fun deleteAll() {
        mDeviceInfoDao.deleteAll()
    }
}