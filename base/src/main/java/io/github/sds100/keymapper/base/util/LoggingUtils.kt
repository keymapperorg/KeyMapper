package io.github.sds100.keymapper.base.util

import timber.log.Timber
import kotlin.system.measureTimeMillis



inline fun <T> logMeasureTimeMillis(block: () -> T): T {
    val result: T
    val time = measureTimeMillis {
        result = block.invoke()
    }

    Timber.e("$time ms")

    return result
}
