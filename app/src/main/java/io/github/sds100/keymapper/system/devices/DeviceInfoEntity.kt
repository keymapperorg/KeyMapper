package io.github.sds100.keymapper.system.devices

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.db.dao.DeviceInfoDao
import io.github.sds100.keymapper.data.db.dao.DeviceInfoDao.Companion.KEY_DESCRIPTOR
import io.github.sds100.keymapper.data.db.dao.DeviceInfoDao.Companion.KEY_NAME

/**
 * Created by sds100 on 18/05/2020.
 */
@Entity(tableName = DeviceInfoDao.TABLE_NAME)
data class DeviceInfoEntity(
    @PrimaryKey
    @SerializedName(NAME_DESCRIPTOR)
    @ColumnInfo(name = KEY_DESCRIPTOR)
    val descriptor: String,

    @ColumnInfo(name = KEY_NAME)
    @SerializedName(NAME_NAME)
    val name: String) {

    companion object {
        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_DESCRIPTOR = "descriptor"
        const val NAME_NAME = "name"

        val DESERIALIZER = jsonDeserializer {
            val descriptor by it.json.byString(NAME_DESCRIPTOR)
            val name by it.json.byString(NAME_NAME)

            DeviceInfoEntity(descriptor, name)
        }
    }
}
