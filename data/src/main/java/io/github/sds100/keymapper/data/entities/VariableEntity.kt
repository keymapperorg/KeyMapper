package io.github.sds100.keymapper.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.sds100.keymapper.data.db.dao.VariableDao

@Entity(tableName = VariableDao.TABLE_NAME)
data class VariableEntity(
    @PrimaryKey
    @ColumnInfo(name = VariableDao.KEY_NAME)
    val name: String,
    @ColumnInfo(name = VariableDao.KEY_VALUE)
    val value: Long,
)
