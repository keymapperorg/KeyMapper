package io.github.sds100.keymapper.util

import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Created by sds100 on 19/04/2021.
 */
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
