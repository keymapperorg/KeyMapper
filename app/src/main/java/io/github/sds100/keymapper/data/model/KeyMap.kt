package io.github.sds100.keymapper.data.model

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.util.str
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 12/07/2018.
 */

@Entity(tableName = KeyMapDao.TABLE_NAME)
class KeyMap(
    @PrimaryKey(autoGenerate = true)
    var id: Long,

    @ColumnInfo(name = KeyMapDao.KEY_TRIGGER)
    var trigger: Trigger = Trigger(),

    @ColumnInfo(name = KeyMapDao.KEY_ACTION_LIST)
    var actionList: List<Action> = listOf(),

    @ColumnInfo(name = KeyMapDao.KEY_CONSTRAINT_LIST)
    var constraintList: List<Constraint> = listOf(),

    @ConstraintMode
    @ColumnInfo(name = KeyMapDao.KEY_CONSTRAINT_MODE)
    var constraintMode: Int = Constraint.DEFAULT_MODE,

    @ColumnInfo(name = KeyMapDao.KEY_FLAGS)
    /**
     * Flags are stored as bits.
     */
    var flags: Int = 0,

    @ColumnInfo(name = KeyMapDao.KEY_FOLDER_NAME)
    var folderName: String? = null,

    @ColumnInfo(name = KeyMapDao.KEY_ENABLED)
    var isEnabled: Boolean = true
) {
    companion object {
        //DON'T CHANGE THESE AND THEY MUST BE POWERS OF 2!!
        const val KEYMAP_FLAG_VIBRATE = 1
        const val KEYMAP_FLAG_SHOW_PERFORMING_ACTION_TOAST = 2
        const val KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION = 4

        val KEYMAP_FLAG_LABEL_MAP = mapOf(
            KEYMAP_FLAG_VIBRATE to R.string.flag_vibrate,
            KEYMAP_FLAG_SHOW_PERFORMING_ACTION_TOAST to R.string.flag_performing_action_toast,
            KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION to R.string.flag_long_press_double_vibration
        )

        val KEYMAP_FLAG_ICON_MAP = mapOf(
            KEYMAP_FLAG_VIBRATE to R.drawable.ic_outline_vibration_24,
            KEYMAP_FLAG_SHOW_PERFORMING_ACTION_TOAST to R.drawable.ic_baseline_announcement_24,
            KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION to R.drawable.ic_outline_vibration_24
        )

        fun getFlagLabelList(ctx: Context, flags: Int): List<String> = sequence {
            KEYMAP_FLAG_LABEL_MAP.keys.forEach { flag ->
                if (flags.hasFlag(flag)) {
                    yield(ctx.str(KEYMAP_FLAG_LABEL_MAP.getValue(flag)))
                }
            }
        }.toList()
    }

    override fun hashCode() = id.toInt()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyMap

        if (id != other.id) return false

        return true
    }

    fun clone(): KeyMap = KeyMap(0, trigger, actionList, constraintList, constraintMode, flags, folderName, isEnabled)
}