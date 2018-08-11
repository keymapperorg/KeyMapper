package io.github.sds100.keymapper

import android.content.Context
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT

/**
 * Created by sds100 on 12/07/2018.
 */

data class KeyMap(
        val id: Long,
        val triggerList: MutableList<Trigger> = mutableListOf(),
        val action: Action
) {
    override fun hashCode() = id.toInt()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyMap

        if (id != other.id) return false

        return true
    }

    /**
     * [ctx] is required so toast messages can be displayed
     */
    fun isValid(ctx: Context): Boolean {
        if (triggerList.isEmpty()) {
            Toast.makeText(ctx, R.string.error_must_have_atleast_one_trigger, LENGTH_SHORT).show()
            return false
        }

        return true
    }
}