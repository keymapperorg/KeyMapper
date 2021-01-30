package io.github.sds100.keymapper.data.repository

import io.github.sds100.keymapper.data.db.dao.DeviceInfoDao
import io.github.sds100.keymapper.data.model.DeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 26/01/2020.
 */
class DefaultDeviceInfoRepository(private val deviceInfoDao: DeviceInfoDao,
                                  private val coroutineScope: CoroutineScope) : DeviceInfoRepository {

    override suspend fun getAll(): List<DeviceInfo> {
        return deviceInfoDao.getAll()
    }

    override fun insertDeviceInfo(vararg deviceInfo: DeviceInfo) {
        coroutineScope.launch {
            deviceInfoDao.insert(*deviceInfo)
        }
    }

    override fun getDeviceInfo(descriptor: String) {
        coroutineScope.launch {
            deviceInfoDao.getByDescriptor(descriptor)
        }
    }

    override fun deleteAll() {
        coroutineScope.launch {
            deviceInfoDao.deleteAll()
        }
    }
}