package io.github.sds100.keymapper.sysbridge.manager

sealed class SystemBridgeConnectionState {
    /**
     * The time that this connection state was created. This uses SystemClock.elapsedRealtime()
     */
    abstract val time: Long

    data class Connected(override val time: Long) : SystemBridgeConnectionState()

    data class Disconnected(
        override val time: Long,
        /**
         * Whether the user previously stopped the service and that is why it is disconnected.
         * Once the user manually starts it again, this is set to false.
         */
        val isStoppedByUser: Boolean,
    ) : SystemBridgeConnectionState()
}
