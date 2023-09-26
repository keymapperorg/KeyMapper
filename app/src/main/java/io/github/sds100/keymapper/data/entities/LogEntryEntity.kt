package io.github.sds100.keymapper.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.sds100.keymapper.data.db.dao.LogEntryDao

/**
 * Created by sds100 on 13/05/2021.
 */
@Entity(tableName = LogEntryDao.TABLE_NAME)
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = LogEntryDao.KEY_TIME)
    val time: Long,
    @ColumnInfo(name = LogEntryDao.KEY_SEVERITY)
    val severity: Int,
    @ColumnInfo(name = LogEntryDao.KEY_MESSAGE)
    val message: String
) {
    companion object {
        const val SEVERITY_ERROR = 0
        const val SEVERITY_DEBUG = 1
        const val SEVERITY_INFO = 2
    }
}