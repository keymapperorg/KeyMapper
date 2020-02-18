package io.github.sds100.keymapper.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.KeyMapDao
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 12/07/2018.
 */

@Entity(tableName = KeyMapDao.TABLE_NAME)
class KeyMap(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        @ColumnInfo(name = KeyMapDao.KEY_TRIGGER)
        var trigger: Trigger? = null,

        @ColumnInfo(name = KeyMapDao.KEY_ACTION_LIST)
        var actionList: List<Action> = listOf(),

        @ColumnInfo(name = KeyMapDao.KEY_FLAGS)
        /**
         * Flags are stored as bits.
         */
        var flags: Int = 0,

        @ColumnInfo(name = KeyMapDao.KEY_ENABLED)
        var isEnabled: Boolean = true
) {
    companion object {
        //DON'T CHANGE THESE AND THEY MUST BE POWERS OF 2!!
        const val FLAG_LONG_PRESS = 1
        const val FLAG_SHOW_VOLUME_UI = 2
        const val FLAG_VIBRATE = 4

        private val FLAG_LABEL_MAP = mapOf(
                FLAG_LONG_PRESS to R.string.flag_long_press,
                FLAG_SHOW_VOLUME_UI to R.string.flag_show_volume_dialog,
                FLAG_VIBRATE to R.string.flag_vibrate
        )
    }

    val isLongPress
        get() = flags.hasFlag(FLAG_LONG_PRESS)

    override fun hashCode() = id.toInt()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyMap

        if (id != other.id) return false

        return true
    }

    fun clone() = KeyMap(id, trigger, actionList, flags, isEnabled)
}