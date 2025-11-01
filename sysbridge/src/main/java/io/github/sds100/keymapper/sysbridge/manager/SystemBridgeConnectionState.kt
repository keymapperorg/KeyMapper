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
         * Whether the disconnection was expected. E.g the user stopped it or the app is
         * opening for the first time
         */
        val isExpected: Boolean,
    ) : SystemBridgeConnectionState()
}
