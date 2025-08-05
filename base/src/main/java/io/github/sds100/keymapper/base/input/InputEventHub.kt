package io.github.sds100.keymapper.base.input

import android.os.Build
import io.github.sds100.keymapper.sysbridge.IEvdevCallback
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnection
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeManager
import io.github.sds100.keymapper.system.inputevents.KMInputEvent
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputEventHubImpl @Inject constructor(
    private val systemBridgeManager: SystemBridgeManager
) : InputEventHub, IEvdevCallback.Stub() {

    private val callbacks: ConcurrentHashMap<String, CallbackContext> = ConcurrentHashMap()

    private var systemBridge: ISystemBridge? = null

    private val systemBridgeConnection: SystemBridgeConnection = object : SystemBridgeConnection {
        override fun onServiceConnected(service: ISystemBridge) {
            Timber.i("InputEventHub connected to SystemBridge")

            systemBridge = service
        }

        override fun onServiceDisconnected(service: ISystemBridge) {
            Timber.i("InputEventHub disconnected from SystemBridge")

            systemBridge = null
        }

        override fun onBindingDied() {
            Timber.i("SystemBridge connection died")
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemBridgeManager.registerConnection(systemBridgeConnection)
        }
    }

    override fun onEvdevEvent(
        deviceId: Int,
        timeSec: Long,
        timeUsec: Long,
        type: Int,
        code: Int,
        value: Int,
        androidCode: Int
    ) {
        Timber.d(
            "Evdev event: deviceId=${deviceId}, timeSec=$timeSec, timeUsec=$timeUsec, " +
                "type=$type, code=$code, value=$value, androidCode=$androidCode"
        )
    }

    override fun registerClient(
        clientId: String,
        callback: InputEventHubCallback
    ) {
        if (callbacks.contains(clientId)) {
            throw IllegalArgumentException("This client already has a callback registered!")
        }

        callbacks[clientId] = CallbackContext(callback, mutableSetOf())
    }

    override fun unregisterClient(clientId: String) {
        // TODO ungrab the evdev devices
        callbacks.remove(clientId)
    }

    override fun setCallbackDevices(
        clientId: String,
        deviceIds: Array<String>
    ) {
        TODO()
    }

    override fun injectEvent(event: KMInputEvent) {
        TODO()
    }

    private class CallbackContext(
        val callback: InputEventHubCallback,
        /**
         * The input events from devices that this callback subscribes to.
         */
        val deviceIds: MutableSet<Int>
    )
}

interface InputEventHub {
    /**
     * Register a client that will receive input events through the [callback]. The same [clientId]
     * must be used for any requests to other methods in this class. The input events will either
     * come from the key event relay service, accessibility service, or system bridge
     * depending on the type of event and Key Mapper's permissions.
     */
    fun registerClient(clientId: String, callback: InputEventHubCallback)
    fun unregisterClient(clientId: String)

    /**
     * Set the devices that a client wants to listen to.
     */
    fun setCallbackDevices(clientId: String, deviceIds: Array<String>)

    /**
     * Inject an input event. This may either use the key event relay service or the system
     * bridge depending on the permissions granted to Key Mapper.
     */
    fun injectEvent(event: KMInputEvent)
}