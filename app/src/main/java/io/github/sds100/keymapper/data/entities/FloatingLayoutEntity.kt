package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
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
    val uid: String,

    @ColumnInfo(name = KEY_NAME)
    val name: String,
) : Parcelable
