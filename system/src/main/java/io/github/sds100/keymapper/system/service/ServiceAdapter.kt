package io.github.sds100.keymapper.system.service

import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.system.accessibility.ServiceState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [E] is the type for the events to send and receive.
 */
interface ServiceAdapter<E> {
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
    fun acknowledgeCrashed()

    /**
     * Send an event to the service.
     */
    suspend fun send(event: E): Result<*>

    /**
     * Send an event to the service asynchronously. This method
     * will return immediately and you won't be notified of whether it is sent.
     */
    fun sendAsync(event: E)

    /**
     * A flow of events coming from the service.
     */
    val eventReceiver: SharedFlow<E>
}
