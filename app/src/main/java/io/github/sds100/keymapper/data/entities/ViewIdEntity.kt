package io.github.sds100.keymapper.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.sds100.keymapper.data.db.dao.ViewIdDao

@Entity(tableName = ViewIdDao.TABLE_NAME)
data class ViewIdEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = ViewIdDao.KEY_VIEW_ID)
    val viewId: String,
    @ColumnInfo(name = ViewIdDao.KEY_PACKAGE_NAME)
    val packageName: String,
    @ColumnInfo(name = ViewIdDao.KEY_FULL_NAME,)
    val fullName: String,
){
    companion object{
        const val NAME_ID = "id"
        const val NAME_VIEW_ID = "viewId"
        const val NAME_PACKAGE_NAME = "packageName"
        const val NAME_FULL_NAME = "fullName"
    }
}