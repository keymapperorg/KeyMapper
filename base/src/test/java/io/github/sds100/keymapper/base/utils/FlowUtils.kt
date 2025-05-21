package io.github.sds100.keymapper.base.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull

object FlowUtils {
    /**
     * This is useful for collecting SharedFlow in tests.
     */
    suspend fun <T> Flow<T>.toListWithTimeout(timeout: Long = 1000L): List<T> {
        val items = mutableListOf<T>()

        withTimeoutOrNull(timeout) {
            this@toListWithTimeout.toList(items)
        }

        return items
    }
}
