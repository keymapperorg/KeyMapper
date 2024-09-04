package io.github.sds100.keymapper.util

import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * Created by sds100 on 19/01/21.
 */

inline fun <T> logMeasureTimeMillis(block: () -> T): T {
    val result: T
    val time = measureTimeMillis {
        result = block.invoke()
    }

    Timber.e("$time ms")

    return result
}
