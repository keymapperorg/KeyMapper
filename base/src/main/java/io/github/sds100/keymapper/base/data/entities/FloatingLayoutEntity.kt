package io.github.sds100.keymapper.base.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.db.dao.FloatingLayoutDao.Companion.KEY_NAME
import io.github.sds100.keymapper.data.db.dao.FloatingLayoutDao.Companion.KEY_UID
import io.github.sds100.keymapper.data.db.dao.FloatingLayoutDao.Companion.TABLE_NAME
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = TABLE_NAME,
    indices = [Index(value = [KEY_NAME], unique = true)],
)
@Parcelize
data class FloatingLayoutEntity(
    @PrimaryKey
    @ColumnInfo(name = KEY_UID)
    @SerializedName(NAME_UID)
    val uid: String,

    @ColumnInfo(name = KEY_NAME)
    @SerializedName(NAME_NAME)
    val name: String,
) : Parcelable {
    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_UID = "uid"
        const val NAME_NAME = "name"

        val DESERIALIZER = jsonDeserializer {
            val uid by it.json.byString(NAME_UID)
            val name by it.json.byString(NAME_NAME)

            FloatingLayoutEntity(uid, name)
        }
    }
}
