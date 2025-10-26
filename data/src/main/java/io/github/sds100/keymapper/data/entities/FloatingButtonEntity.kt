package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byNullableBool
import com.github.salomonbrys.kotson.byNullableFloat
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_BACKGROUND_OPACITY
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_BORDER_OPACITY
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_BUTTON_SIZE
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_DISPLAY_HEIGHT
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_DISPLAY_WIDTH
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_LAYOUT_UID
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_ORIENTATION
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_SHOW_OVER_INPUT_METHOD
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao.Companion.KEY_SHOW_OVER_STATUS_BAR
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
    @SerializedName(NAME_UID)
    val uid: String,
    @ColumnInfo(name = KEY_LAYOUT_UID, index = true)
    @SerializedName(NAME_LAYOUT_UID)
    val layoutUid: String,
    @ColumnInfo(name = KEY_TEXT)
    @SerializedName(NAME_TEXT)
    val text: String,
    @ColumnInfo(name = KEY_BUTTON_SIZE)
    @SerializedName(NAME_BUTTON_SIZE)
    val buttonSize: Int,
    @ColumnInfo(name = KEY_X)
    @SerializedName(NAME_X)
    val x: Int,
    @ColumnInfo(name = KEY_Y)
    @SerializedName(NAME_Y)
    val y: Int,
    @ColumnInfo(name = KEY_ORIENTATION)
    @SerializedName(NAME_ORIENTATION)
    val orientation: String,
    @ColumnInfo(name = KEY_DISPLAY_WIDTH)
    @SerializedName(NAME_DISPLAY_WIDTH)
    val displayWidth: Int,
    @ColumnInfo(name = KEY_DISPLAY_HEIGHT)
    @SerializedName(NAME_DISPLAY_HEIGHT)
    val displayHeight: Int,
    @ColumnInfo(name = KEY_BORDER_OPACITY)
    @SerializedName(NAME_BORDER_OPACITY)
    val borderOpacity: Float?,
    @ColumnInfo(name = KEY_BACKGROUND_OPACITY)
    @SerializedName(NAME_BACKGROUND_OPACITY)
    val backgroundOpacity: Float?,
    @ColumnInfo(name = KEY_SHOW_OVER_STATUS_BAR)
    @SerializedName(NAME_SHOW_OVER_STATUS_BAR)
    val showOverStatusBar: Boolean?,
    @ColumnInfo(name = KEY_SHOW_OVER_INPUT_METHOD)
    @SerializedName(NAME_SHOW_OVER_INPUT_METHOD)
    val showOverInputMethod: Boolean?,
) : Parcelable {
    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_UID = "uid"
        const val NAME_LAYOUT_UID = "layoutUid"
        const val NAME_TEXT = "text"
        const val NAME_BUTTON_SIZE = "buttonSize"
        const val NAME_X = "x"
        const val NAME_Y = "y"
        const val NAME_ORIENTATION = "orientation"
        const val NAME_DISPLAY_WIDTH = "displayWidth"
        const val NAME_DISPLAY_HEIGHT = "displayHeight"
        const val NAME_BORDER_OPACITY = "border_opacity"
        const val NAME_BACKGROUND_OPACITY = "background_opacity"
        const val NAME_SHOW_OVER_STATUS_BAR = "show_over_status_bar"
        const val NAME_SHOW_OVER_INPUT_METHOD = "show_over_input_method"

        val DESERIALIZER =
            jsonDeserializer {
                val uid by it.json.byString(NAME_UID)
                val layoutUid by it.json.byString(NAME_LAYOUT_UID)
                val text by it.json.byString(NAME_TEXT)
                val buttonSize by it.json.byInt(NAME_BUTTON_SIZE)
                val x by it.json.byInt(NAME_X)
                val y by it.json.byInt(NAME_Y)
                val orientation by it.json.byString(NAME_ORIENTATION)
                val displayWidth by it.json.byInt(NAME_DISPLAY_WIDTH)
                val displayHeight by it.json.byInt(NAME_DISPLAY_HEIGHT)
                val borderOpacity by it.json.byNullableFloat(NAME_BORDER_OPACITY)
                val backgroundOpacity by it.json.byNullableFloat(NAME_BACKGROUND_OPACITY)
                val showOverStatusBar by it.json.byNullableBool(NAME_SHOW_OVER_STATUS_BAR)
                val showOverInputMethod by it.json.byNullableBool(NAME_SHOW_OVER_INPUT_METHOD)

                FloatingButtonEntity(
                    uid = uid,
                    layoutUid = layoutUid,
                    text = text,
                    buttonSize = buttonSize,
                    x = x,
                    y = y,
                    orientation = orientation,
                    displayWidth = displayWidth,
                    displayHeight = displayHeight,
                    borderOpacity = borderOpacity,
                    backgroundOpacity = backgroundOpacity,
                    showOverStatusBar = showOverStatusBar,
                    showOverInputMethod = showOverInputMethod,
                )
            }
    }
}
