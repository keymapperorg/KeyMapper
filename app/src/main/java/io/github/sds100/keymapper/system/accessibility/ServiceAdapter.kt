package io.github.sds100.keymapper.system.accessibility

import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 17/03/2021.
 */
interface ServiceAdapter {
    val state: StateFlow<ServiceState>

    /**
     * @return Whether the service could be started successfully.
     */
    fun start(): Boolean

    /**
     * @return Whether the service could be restarted successfully.
     */
    fun restart(): Boolean

    /**
     * @return Whether the service could be restarted successfully.
     */
    fun stop(): Boolean

    suspend fun isCrashed(): Boolean

    suspend fun send(event: Event): Result<*>
    val eventReceiver: SharedFlow<Event>
}