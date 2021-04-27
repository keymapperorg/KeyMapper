package io.github.sds100.keymapper.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.sds100.keymapper.system.devices.DeviceInfoEntity

/**
 * Created by sds100 on 18/02/20.
 */

@Dao
interface DeviceInfoDao {
    companion object {
        const val TABLE_NAME = "deviceinfo"
        const val KEY_DESCRIPTOR = "descriptor"
        const val KEY_NAME = "name"
    }

    @Query("SELECT * FROM $TABLE_NAME")
    suspend fun getAll(): List<DeviceInfoEntity>

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_DESCRIPTOR = (:descriptor)")
    suspend fun getByDescriptor(descriptor: String): DeviceInfoEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg deviceInfo: DeviceInfoEntity)

    @Query("DELETE FROM ${KeyMapDao.TABLE_NAME}")
    suspend fun deleteAll()
}