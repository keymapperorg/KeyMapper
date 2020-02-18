package io.github.sds100.keymapper.data.model

import androidx.annotation.IntDef

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [keys] The key codes which will trigger the action
 */
data class Trigger(var keys: List<Key>, @Mode var mode: Int = DEFAULT_TRIGGER_MODE) {

    companion object {
        const val PARALLEL = 0
        const val SEQUENCE = 1

        const val DEFAULT_TRIGGER_MODE = PARALLEL
    }

    data class Key(val keyCode: Int, val deviceId: String? = null) {
        override fun equals(other: Any?): Boolean {
            return (other as Key).keyCode == keyCode
        }

        override fun hashCode() = keyCode
    }

    @IntDef(value = [PARALLEL, SEQUENCE])
    annotation class Mode
}