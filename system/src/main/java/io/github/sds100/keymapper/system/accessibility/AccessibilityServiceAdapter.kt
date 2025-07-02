package io.github.sds100.keymapper.system.accessibility

import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AccessibilityServiceAdapter {
    val state: StateFlow<AccessibilityServiceState>
    fun invalidateState()

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
    suspend fun send(event: AccessibilityServiceEvent): KMResult<*>

    /**
     * Send an event to the service asynchronously. This method
     * will return immediately and you won't be notified of whether it is sent.
     */
    fun sendAsync(event: AccessibilityServiceEvent)

    /**
     * A flow of events coming from the service.
     */
    val eventReceiver: SharedFlow<AccessibilityServiceEvent>
}
