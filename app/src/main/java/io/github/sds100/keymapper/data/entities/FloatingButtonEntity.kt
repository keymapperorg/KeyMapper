package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_BUTTON_SIZE
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_DISPLAY_HEIGHT
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_DISPLAY_WIDTH
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_LAYOUT_UID
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_ORIENTATION
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_TEXT
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_UID
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_X
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_Y
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.TABLE_NAME
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = FloatingLayoutEntity::class,
            parentColumns = [KEY_UID],
            childColumns = [KEY_LAYOUT_UID],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Parcelize
data class FloatingButtonEntity(
    @PrimaryKey
    @ColumnInfo(name = KEY_UID)
    val uid: String,

    @ColumnInfo(name = KEY_LAYOUT_UID)
    val layoutUid: String,

    @ColumnInfo(name = KEY_TEXT)
    val text: String,

    @ColumnInfo(name = KEY_BUTTON_SIZE)
    val buttonSize: Int,

    @ColumnInfo(name = KEY_X)
    val x: Int,

    @ColumnInfo(name = KEY_Y)
    val y: Int,

    @ColumnInfo(name = KEY_ORIENTATION)
    val orientation: String,

    @ColumnInfo(name = KEY_DISPLAY_WIDTH)
    val displayWidth: Int,

    @ColumnInfo(name = KEY_DISPLAY_HEIGHT)
    val displayHeight: Int,
) : Parcelable
