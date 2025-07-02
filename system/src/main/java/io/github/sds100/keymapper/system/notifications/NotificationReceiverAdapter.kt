package io.github.sds100.keymapper.system.notifications

import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface NotificationReceiverAdapter {
    val isEnabled: StateFlow<Boolean>

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

    /**
     * Send an event to the service.
     */
    suspend fun send(event: NotificationServiceEvent): KMResult<*>

    /**
     * Send an event to the service asynchronously. This method
     * will return immediately and you won't be notified of whether it is sent.
     */
    fun sendAsync(event: NotificationServiceEvent)

    /**
     * A flow of events coming from the service.
     */
    val eventReceiver: SharedFlow<NotificationServiceEvent>
}
