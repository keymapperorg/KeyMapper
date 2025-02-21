package io.github.sds100.keymapper.system.accessibility

import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.ServiceEvent
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

    /**
     * Send an event to the service.
     */
    suspend fun send(event: ServiceEvent): Result<*>

    /**
     * Send an event to the service asynchronously. This method
     * will return immediately and you won't be notified of whether it is sent.
     */
    fun sendAsync(event: ServiceEvent)

    /**
     * A flow of events coming from the service.
     */
    val eventReceiver: SharedFlow<ServiceEvent>
}
