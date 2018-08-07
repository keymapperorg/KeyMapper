package io.github.sds100.keymapper

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

}