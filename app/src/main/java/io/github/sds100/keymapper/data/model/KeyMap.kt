package io.github.sds100.keymapper.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.KeyMapDao
import splitties.bitflags.hasFlag
import splitties.resources.appDrawable
import splitties.resources.appStr

/**
 * Created by sds100 on 12/07/2018.
 */

@Entity(tableName = KeyMapDao.TABLE_NAME)
class KeyMap(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    @ColumnInfo(name = KeyMapDao.KEY_TRIGGER)
    var trigger: Trigger = Trigger(),

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
        const val KEYMAP_FLAG_VIBRATE = 1

        private val KEYMAP_FLAG_LABEL_MAP = mapOf(
            KEYMAP_FLAG_VIBRATE to R.string.flag_vibrate
        )

        private val KEYMAP_FLAG_ICON_MAP = mapOf(
            KEYMAP_FLAG_VIBRATE to R.drawable.ic_outline_vibration_24
        )

        fun getFlagLabelList(flags: Int): List<String> = sequence {
            KEYMAP_FLAG_LABEL_MAP.keys.forEach { flag ->
                if (flags.hasFlag(flag)) {
                    yield(appStr(KEYMAP_FLAG_LABEL_MAP.getValue(flag)))
                }
            }
        }.toList()

        fun createFlagModels(flags: Int): List<FlagModel> = sequence {
            KEYMAP_FLAG_LABEL_MAP.keys.forEach { flag ->
                if (flags.hasFlag(flag)) {
                    val text = appStr(KEYMAP_FLAG_LABEL_MAP.getValue(flag))
                    val drawable = appDrawable(KEYMAP_FLAG_ICON_MAP.getValue(flag))
                    yield(FlagModel(text, drawable))
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

    fun clone() = KeyMap(id, trigger, actionList, flags, isEnabled)
}