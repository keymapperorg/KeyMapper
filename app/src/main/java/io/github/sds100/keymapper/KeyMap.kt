package io.github.sds100.keymapper

/**
 * Created by sds100 on 12/07/2018.
 */

data class KeyMap(
        val id: Long,
        val triggerList: MutableList<Trigger> = mutableListOf(),
        val action: Action
)