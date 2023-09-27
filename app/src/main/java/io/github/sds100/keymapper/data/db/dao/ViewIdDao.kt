package io.github.sds100.keymapper.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import io.github.sds100.keymapper.data.entities.ViewIdEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ViewIdDao {
    companion object {
        const val TABLE_NAME = "viewids"
        const val KEY_ID = "id"
        const val KEY_ELEMENT_ID = "view_id"
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_FULL_NAME = "full_name"
    }

    @Query("SELECT * FROM $TABLE_NAME ORDER BY $KEY_PACKAGE_NAME ASC, $KEY_ELEMENT_ID ASC")
    fun getAll(): Flow<List<ViewIdEntity>>

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_ID = :id")
    fun getById(id: Long): ViewIdEntity

    @Query("SELECT EXISTS(SELECT * FROM $TABLE_NAME WHERE $KEY_FULL_NAME = :fullName)")
    fun viewIdExists(fullName: String): Boolean

    @Query("DELETE FROM $TABLE_NAME")
    suspend fun deleteAll()

    @Query("UPDATE sqlite_sequence SET seq=0 WHERE name='$TABLE_NAME'")
    suspend fun resetDb()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(vararg entry: ViewIdEntity)
}