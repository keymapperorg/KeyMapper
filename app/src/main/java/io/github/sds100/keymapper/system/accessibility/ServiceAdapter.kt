package io.github.sds100.keymapper.system.accessibility

import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 17/03/2021.
 */
interface ServiceAdapter {
    val isEnabled: StateFlow<Boolean>

    fun enableService()
    fun restartService()
    fun disableService()

    suspend fun send(event: Event): Result<*>
    val eventReceiver: SharedFlow<Event>
}