package io.github.sds100.keymapper.base.input

import android.os.Build
import android.os.RemoteException
import android.view.KeyEvent
import io.github.sds100.keymapper.base.BuildConfig
import io.github.sds100.keymapper.base.system.inputmethod.ImeInputEventInjector
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.IEvdevCallback
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnection
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.utils.SystemBridgeError
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputevents.KMEvdevEvent
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.inputevents.KMInputEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputEventHubImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val imeInputEventInjector: ImeInputEventInjector,
    private val preferenceRepository: PreferenceRepository,
    private val devicesAdapter: DevicesAdapter,
) : InputEventHub, IEvdevCallback.Stub() {

    companion object {
        private const val INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2
    }

    private val clients: ConcurrentHashMap<String, ClientContext> = ConcurrentHashMap()

    private var systemBridgeFlow: MutableStateFlow<ISystemBridge?> = MutableStateFlow(null)

    // Event queue for processing key events asynchronously in order
    private val keyEventQueue = Channel<InjectKeyEventModel>(capacity = 100)

    private val systemBridgeConnection: SystemBridgeConnection = object : SystemBridgeConnection {
        override fun onServiceConnected(service: ISystemBridge) {
            Timber.i("InputEventHub connected to SystemBridge")

            systemBridgeFlow.update { service }
            service.registerEvdevCallback(this@InputEventHubImpl)
        }

        override fun onServiceDisconnected(service: ISystemBridge) {
            Timber.i("InputEventHub disconnected from SystemBridge")

            systemBridgeFlow.update { null }
        }

        override fun onBindingDied() {
            Timber.i("SystemBridge connection died")
        }
    }

    private val evdevHandles: EvdevHandleCache = EvdevHandleCache(
        coroutineScope,
        devicesAdapter,
        systemBridgeFlow,
    )

    private val logInputEventsEnabled: StateFlow<Boolean> =
        preferenceRepository.get(Keys.log).map { isLogEnabled ->
            if (isLogEnabled == true) {
                isLogEnabled
            } else {
                BuildConfig.DEBUG
            }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemBridgeConnectionManager.registerConnection(systemBridgeConnection)
        }

        startKeyEventProcessingLoop()
    }

    /**
     * Starts a coroutine that processes key events from the queue in order on the IO thread.=
     **/
    private fun startKeyEventProcessingLoop() {
        coroutineScope.launch(Dispatchers.IO) {
            for (event in keyEventQueue) {
                try {
                    injectKeyEvent(event)
                } catch (e: Exception) {
                    Timber.e(e, "Error processing key event: $event")
                }
            }
        }
    }

    override fun isSystemBridgeConnected(): Boolean {
        return systemBridgeFlow.value != null
    }

    override fun onEvdevEventLoopStarted() {
        Timber.i("On evdev event loop started")
        invalidateGrabbedEvdevDevices()
    }

    override fun onEvdevEvent(
        devicePath: String?,
        timeSec: Long,
        timeUsec: Long,
        type: Int,
        code: Int,
        value: Int,
        androidCode: Int,
    ): Boolean {
        devicePath ?: return false

        val handle = evdevHandles.getByPath(devicePath) ?: return false
        val evdevEvent = KMEvdevEvent(handle, type, code, value, androidCode, timeSec, timeUsec)

        return onInputEvent(evdevEvent, InputEventDetectionSource.EVDEV)
    }

    override fun onInputEvent(
        event: KMInputEvent,
        detectionSource: InputEventDetectionSource,
    ): Boolean {
        if (logInputEventsEnabled.value) {
            logInputEvent(event)
        }

        var consume = false

        for (clientContext in clients.values) {
            if (event is KMEvdevEvent) {
                // TODO maybe flatmap all the client event types into one Set so this check
                // can be done in onEvdevEvent. Hundreds of events may be sent per second synchronously so important to be as fast as possible.
                // This client can ignore this event.
                if (!clientContext.evdevEventTypes.contains(event.type) ||
                    clientContext.grabbedEvdevDevices.isEmpty()
                ) {
                    continue
                }

                val deviceInfo = EvdevDeviceInfo(
                    event.device.name,
                    event.device.bus,
                    event.device.vendor,
                    event.device.product,
                )

                // Only send events from evdev devices to the client if they grabbed it
                if (!clientContext.grabbedEvdevDevices.contains(deviceInfo)) {
                    continue
                }

                // Lazy evaluation may not execute this if its inlined
                val result = clientContext.callback.onInputEvent(event, detectionSource)
                consume = consume || result
            } else {
                // Lazy evaluation may not execute this if its inlined
                val result = clientContext.callback.onInputEvent(event, detectionSource)
                consume = consume || result
            }
        }

        if (logInputEventsEnabled.value) {
            Timber.d("Consumed: $consume")
        }

        return consume
    }

    private fun logInputEvent(event: KMInputEvent) {
        when (event) {
            is KMEvdevEvent -> {
                Timber.d(
                    "Evdev event: devicePath=${event.device.path}, deviceName=${event.device.name}, type=${event.type}, code=${event.code}, value=${event.value}",
                )
            }

            is KMGamePadEvent -> {
                Timber.d(
                    "GamePad event: deviceId=${event.deviceId}, axisHatX=${event.axisHatX}, axisHatY=${event.axisHatY}",
                )
            }

            is KMKeyEvent -> {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        Timber.d("Key down ${KeyEvent.keyCodeToString(event.keyCode)}: keyCode=${event.keyCode}, scanCode=${event.scanCode}, deviceId=${event.deviceId}, metaState=${event.metaState}, source=${event.source}")
                    }

                    KeyEvent.ACTION_UP -> {
                        Timber.d("Key up ${KeyEvent.keyCodeToString(event.keyCode)}: keyCode=${event.keyCode}, scanCode=${event.scanCode}, deviceId=${event.deviceId}, metaState=${event.metaState}, source=${event.source}")
                    }

                    else -> {
                        Timber.w("Unknown key event action: ${event.action}")
                    }
                }
            }
        }
    }

    override fun registerClient(
        clientId: String,
        callback: InputEventHubCallback,
        eventTypes: List<Int>,
    ) {
        if (clients.containsKey(clientId)) {
            throw IllegalArgumentException("This client already has a callback registered!")
        }

        clients[clientId] = ClientContext(callback, emptySet(), eventTypes.toSet())
    }

    override fun unregisterClient(clientId: String) {
        clients.remove(clientId)
        invalidateGrabbedEvdevDevices()
    }

    override fun setGrabbedEvdevDevices(clientId: String, devices: List<EvdevDeviceInfo>) {
        if (!clients.containsKey(clientId)) {
            throw IllegalArgumentException("This client $clientId is not registered when trying to grab devices!")
        }

        clients[clientId] = clients[clientId]!!.copy(grabbedEvdevDevices = devices.toSet())

        invalidateGrabbedEvdevDevices()
    }

    override fun grabAllEvdevDevices(clientId: String) {
        if (!clients.containsKey(clientId)) {
            throw IllegalArgumentException("This client $clientId is not registered when trying to grab devices!")
        }

        val devices = evdevHandles.getDevices().toSet()
        clients[clientId] = clients[clientId]!!.copy(grabbedEvdevDevices = devices)

        invalidateGrabbedEvdevDevices()
    }

    // TODO invalidate when the input devices change
    private fun invalidateGrabbedEvdevDevices() {
        val evdevDevices: Set<EvdevDeviceInfo> =
            clients.values.flatMap { it.grabbedEvdevDevices }.toSet()

        val systemBridge = systemBridgeFlow.value

        if (systemBridge == null) {
            return
        }

        // Grabbing can block if there are other grabbing or event loop start/stop operations happening.
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val ungrabResult = systemBridge.ungrabAllEvdevDevices()
                Timber.i("Ungrabbed all evdev devices: $ungrabResult")

                if (!ungrabResult) {
                    Timber.e("Failed to ungrab all evdev devices before grabbing.")
                    return@launch
                }

                for (device in evdevDevices) {
                    val handle = evdevHandles.getByInfo(device) ?: continue
                    val grabResult = systemBridge.grabEvdevDevice(handle.path)

                    Timber.i("Grabbed evdev device ${device.name}: $grabResult")
                }
            } catch (_: RemoteException) {
                Timber.e("Failed to invalidate grabbed device. Is the system bridge dead?")
            }
        }
    }

    override fun injectEvdevEvent(
        devicePath: String,
        type: Int,
        code: Int,
        value: Int,
    ): KMResult<Boolean> {
        val systemBridge = this.systemBridgeFlow.value

        if (systemBridge == null) {
            Timber.w("System bridge is not connected, cannot inject evdev event.")
            return SystemBridgeError.Disconnected
        }

        try {
            val result = systemBridge.writeEvdevEvent(
                devicePath,
                type,
                code,
                value,
            )

            Timber.d("Injected evdev event: $result")

            return Success(result)
        } catch (e: RemoteException) {
            Timber.e(e, "Failed to inject evdev event")
            return KMError.Exception(e)
        }
    }

    override suspend fun injectKeyEvent(event: InjectKeyEventModel): KMResult<Unit> {
        val systemBridge = this.systemBridgeFlow.value

        if (systemBridge == null) {
            imeInputEventInjector.inputKeyEvent(event)
            return Success(Unit)
        } else {
            try {
                val androidKeyEvent = event.toAndroidKeyEvent(flags = KeyEvent.FLAG_FROM_SYSTEM)

                if (logInputEventsEnabled.value) {
                    Timber.d("Injecting key event $androidKeyEvent with system bridge")
                }

                withContext(Dispatchers.IO) {
                    // All injected events have their device id set to -1 (VIRTUAL_KEYBOARD_ID)
                    // in InputDispatcher.cpp injectInputEvent.
                    systemBridge.injectInputEvent(
                        androidKeyEvent,
                        INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH,
                    )
                }

                return Success(Unit)
            } catch (e: RemoteException) {
                return KMError.Exception(e)
            }
        }
    }

    override fun injectKeyEventAsync(event: InjectKeyEventModel): KMResult<Unit> {
        return try {
            // Send the event to the queue for processing
            if (keyEventQueue.trySend(event).isSuccess) {
                Success(Unit)
            } else {
                KMError.Exception(Exception("Failed to queue key event"))
            }
        } catch (e: Exception) {
            KMError.Exception(e)
        }
    }

    private data class ClientContext(
        val callback: InputEventHubCallback,
        /**
         * The evdev devices that this client wants to grab.
         */
        val grabbedEvdevDevices: Set<EvdevDeviceInfo>,
        val evdevEventTypes: Set<Int>,
    )
}

interface InputEventHub {
    /**
     * Register a client that will receive input events through the [callback]. The same [clientId]
     * must be used for any requests to other methods in this class. The input events will either
     * come from the key event relay service, accessibility service, or system bridge
     * depending on the type of event and Key Mapper's permissions.
     */
    fun registerClient(
        clientId: String,
        callback: InputEventHubCallback,
        eventTypes: List<Int>,
    )

    fun unregisterClient(clientId: String)

    fun setGrabbedEvdevDevices(clientId: String, devices: List<EvdevDeviceInfo>)
    fun grabAllEvdevDevices(clientId: String)

    /**
     * Inject a key event. This may either use the key event relay service or the system
     * bridge depending on the permissions granted to Key Mapper.
     *
     * Must be suspend so injecting to the systembridge can happen on another thread.
     */
    suspend fun injectKeyEvent(event: InjectKeyEventModel): KMResult<Unit>

    /**
     * Some callers don't care about the result from injecting and it isn't critical
     * for it to be synchronous so calls to this
     * will be put in a queue and processed.
     */
    fun injectKeyEventAsync(event: InjectKeyEventModel): KMResult<Unit>

    fun injectEvdevEvent(devicePath: String, type: Int, code: Int, value: Int): KMResult<Boolean>

    /**
     * Send an input event to the connected clients.
     *
     * @return whether the input event was consumed by a client.
     */
    fun onInputEvent(event: KMInputEvent, detectionSource: InputEventDetectionSource): Boolean

    fun isSystemBridgeConnected(): Boolean
}
