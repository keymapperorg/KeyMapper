package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byNullableLong
import com.github.salomonbrys.kotson.byNullableString
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.db.dao.GroupDao
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Entity(
    tableName = GroupDao.TABLE_NAME,
    indices = [Index(value = [GroupDao.KEY_NAME], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = [GroupDao.KEY_UID],
            childColumns = [GroupDao.KEY_PARENT_UID],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Parcelize
data class GroupEntity(
    @PrimaryKey
    @ColumnInfo(name = GroupDao.KEY_UID)
    @SerializedName(NAME_UID)
    val uid: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = GroupDao.KEY_NAME)
    @SerializedName(NAME_NAME)
    val name: String,

    @ColumnInfo(name = GroupDao.KEY_CONSTRAINTS)
    @SerializedName(NAME_CONSTRAINTS)
    val constraintList: List<ConstraintEntity> = emptyList(),

    @ColumnInfo(name = GroupDao.KEY_CONSTRAINT_MODE)
    @SerializedName(NAME_CONSTRAINT_MODE)
    val constraintMode: Int = ConstraintEntity.MODE_AND,

    @ColumnInfo(name = GroupDao.KEY_PARENT_UID)
    @SerializedName(NAME_PARENT_UID)
    val parentUid: String?,

    @ColumnInfo(name = GroupDao.KEY_LAST_OPENED_DATE)
    @SerializedName(NAME_LAST_OPENED_DATE)
    val lastOpenedDate: Long?,

) : Parcelable {
    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_UID = "uid"
        const val NAME_NAME = "name"
        const val NAME_CONSTRAINTS = "constraints"
        const val NAME_CONSTRAINT_MODE = "constraint_mode"
        const val NAME_PARENT_UID = "parent_uid"
        const val NAME_LAST_OPENED_DATE = "last_opened_date"

        val DESERIALIZER = jsonDeserializer {
            val uid by it.json.byString(NAME_UID)
            val name by it.json.byString(NAME_NAME)
            val constraintListJsonArray by it.json.byArray(NAME_CONSTRAINTS)
            val constraintList =
                it.context.deserialize<List<ConstraintEntity>>(constraintListJsonArray)

            val constraintMode by it.json.byInt(NAME_CONSTRAINT_MODE)
            val parentUid by it.json.byNullableString(NAME_PARENT_UID)
            val lastOpenedDate by it.json.byNullableLong(NAME_LAST_OPENED_DATE)

            GroupEntity(uid, name, constraintList, constraintMode, parentUid, lastOpenedDate)
        }
    }
}
