package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.util.Defaultable

/**
 * Created by sds100 on 15/03/2021.
 */
interface SimpleMapping {
    val vibrationDuration: Defaultable<Int>
    val vibrate: Boolean
}
