package io.github.sds100.keymapper.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.sds100.keymapper.data.db.dao.DeviceInfoDao
import io.github.sds100.keymapper.data.db.dao.DeviceInfoDao.Companion.KEY_DESCRIPTOR
import io.github.sds100.keymapper.data.db.dao.DeviceInfoDao.Companion.KEY_NAME

/**
 * Created by sds100 on 18/05/2020.
 */
@Entity(tableName = DeviceInfoDao.TABLE_NAME)
data class DeviceInfo(
    @PrimaryKey
    @ColumnInfo(name = KEY_DESCRIPTOR)
    val descriptor: String,

    @ColumnInfo(name = KEY_NAME)
    val name: String)