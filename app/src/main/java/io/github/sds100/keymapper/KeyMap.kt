package io.github.sds100.keymapper

import android.content.Context
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.sds100.keymapper.Data.KeyMapDao

/**
 * Created by sds100 on 12/07/2018.
 */

@Entity(tableName = KeyMapDao.TABLE_NAME)
data class KeyMap(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        @ColumnInfo(name = KeyMapDao.KEY_TRIGGER_LIST)
        val triggerList: MutableList<Trigger> = mutableListOf(),

        @Embedded
        var action: Action? = null,

        @ColumnInfo(name = KeyMapDao.KEY_FLAGS)
        val flags: MutableList<Int> = mutableListOf(),

        var isEnabled: Boolean = true
) {
    override fun hashCode() = id.toInt()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyMap

        if (id != other.id) return false

        return true
    }
}