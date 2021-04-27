package io.github.sds100.keymapper.domain.utils

import java.util.*

/**
 * Created by sds100 on 11/03/2021.
 */

fun MutableList<*>.moveElement(fromIndex: Int, toIndex: Int) {
    if (fromIndex < toIndex) {
        for (i in fromIndex until toIndex) {
            Collections.swap(this, i, i + 1)
        }
    } else {
        for (i in fromIndex downTo toIndex + 1) {
            Collections.swap(this, i, i - 1)
        }
    }
}