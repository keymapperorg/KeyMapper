package io.github.sds100.keymapper.util

import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * Created by sds100 on 19/01/21.
 */

inline fun logMeasureTimeMillis(block: () -> Unit) {
    Timber.e("${measureTimeMillis(block)}ms")
}