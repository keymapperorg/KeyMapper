package io.github.sds100.keymapper

import java.io.Serializable

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [keys] The key codes which will trigger the action
 */
data class Trigger(val keys: List<Int>) : Serializable