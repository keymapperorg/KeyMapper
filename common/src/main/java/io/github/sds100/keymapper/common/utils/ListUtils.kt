package io.github.sds100.keymapper.common.utils

import java.util.Collections

fun MutableList<*>.moveElement(
    fromIndex: Int,
    toIndex: Int,
) {
    if (toIndex >= size || fromIndex >= size) {
        return
    }

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

inline fun <reified T> Array<out T>.splitIntoBatches(batchSize: Int): Array<Array<out T>> {
    var arrayToSplit = this

    var batches: Array<Array<out T>> = arrayOf()

    while (arrayToSplit.isNotEmpty()) {
        val batch =
            if (arrayToSplit.size < batchSize) {
                arrayToSplit
            } else {
                arrayToSplit.sliceArray(0 until batchSize)
            }

        batches = batches.plus(batch)

        arrayToSplit = arrayToSplit.sliceArray(batch.size until arrayToSplit.size)
    }

    return batches
}
