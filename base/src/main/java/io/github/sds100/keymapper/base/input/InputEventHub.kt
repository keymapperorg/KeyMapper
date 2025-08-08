package io.github.sds100.keymapper.base.input

import android.os.Build
import android.os.RemoteException
import android.view.KeyEvent
import io.github.sds100.keymapper.base.BuildConfig
import io.github.sds100.keymapper.base.system.inputmethod.ImeInputEventInjector
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.success
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.IEvdevCallback
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnection
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeManager
import io.github.sds100.keymapper.sysbridge.utils.SystemBridgeError
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.inputevents.KMEvdevEvent
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.inputevents.KMInputEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputEventHubImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val systemBridgeManager: SystemBridgeManager,
    private val imeInputEventInjector: ImeInputEventInjector,
    private val preferenceRepository: PreferenceRepository,
    private val devicesAdapter: DevicesAdapter,
) : InputEventHub, IEvdevCallback.Stub() {

    companion object {
        private const val INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2
    }

    private val clients: ConcurrentHashMap<String, ClientContext> =
        ConcurrentHashMap()

    private var systemBridge: ISystemBridge? = null

    // Event queue for processing key events asynchronously in order
    private val keyEventQueue = Channel<InjectKeyEventModel>(capacity = 100)

    private val systemBridgeConnection: SystemBridgeConnection = object : SystemBridgeConnection {
        override fun onServiceConnected(service: ISystemBridge) {
            Timber.i("InputEventHub connected to SystemBridge")

            systemBridge = service
            service.registerEvdevCallback(this@InputEventHubImpl)
        }

        override fun onServiceDisconnected(service: ISystemBridge) {
            Timber.i("InputEventHub disconnected from SystemBridge")

            systemBridge = null
        }

        override fun onBindingDied() {
            Timber.i("SystemBridge connection died")
        }
    }

    private val inputDeviceCache: InputDeviceCache =
        InputDeviceCache(coroutineScope, devicesAdapter)
    private val evdevKeyEventTracker: EvdevKeyEventTracker = EvdevKeyEventTracker(inputDeviceCache)

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
            systemBridgeManager.registerConnection(systemBridgeConnection)
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
        return systemBridge != null
    }

    override fun onEvdevEventLoopStarted() {
        Timber.i("On evdev event loop started")
        invalidateGrabbedEvdevDevices()
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
        val evdevEvent = KMEvdevEvent(deviceId, type, code, value, androidCode, timeSec, timeUsec)

        if (logInputEventsEnabled.value) {
            logInputEvent(evdevEvent)
        }

        val keyEvent: KMKeyEvent? = evdevKeyEventTracker.toKeyEvent(evdevEvent)

        if (keyEvent != null) {
            val consumed = onInputEvent(keyEvent, InputEventDetectionSource.EVDEV)

            // Passthrough the key event if it is not consumed.
            if (!consumed) {
                if (logInputEventsEnabled.value) {
                    Timber.d("Passthrough key event from evdev: ${keyEvent.keyCode}")
                }
                runBlocking {
                    injectKeyEvent(
                        InjectKeyEventModel(
                            keyCode = keyEvent.keyCode,
                            action = keyEvent.action,
                            metaState = keyEvent.metaState,
                            deviceId = keyEvent.deviceId,
                            scanCode = keyEvent.scanCode,
                            repeatCount = keyEvent.repeatCount,
                            source = keyEvent.source
                        )
                    )
                }
            }
        } else {
            onInputEvent(evdevEvent, InputEventDetectionSource.EVDEV)
        }
    }

    override fun onInputEvent(
        event: KMInputEvent,
        detectionSource: InputEventDetectionSource
    ): Boolean {
        val uniqueEvent: KMInputEvent = if (event is KMKeyEvent) {
            makeUniqueKeyEvent(event)
        } else {
            event
        }

        if (logInputEventsEnabled.value) {
            logInputEvent(uniqueEvent)
        }

        if (detectionSource == InputEventDetectionSource.EVDEV && event.deviceId == null) {
            return false
        }

        var consume = false

        for (clientContext in clients.values) {
            if (detectionSource == InputEventDetectionSource.EVDEV) {
                val deviceDescriptor = inputDeviceCache.getById(event.deviceId!!)?.descriptor

                // Only send events from evdev devices to the client if they grabbed it

                if (clientContext.grabbedEvdevDevices.contains(deviceDescriptor)) {
                    // Lazy evaluation may not execute this if its inlined?
                    val result = clientContext.callback.onInputEvent(event, detectionSource)
                    consume = consume || result
                }
            } else {
                // Lazy evaluation may not execute this if its inlined?
                val result = clientContext.callback.onInputEvent(event, detectionSource)
                consume = consume || result
            }
        }

        return consume
    }

    /**
     * Sometimes key events are sent with an unknown key code so to make it unique,
     * this will set a unique key code to the key event that won't conflict.
     */
    private fun makeUniqueKeyEvent(event: KMKeyEvent): KMKeyEvent {
        // Guard to ignore processing when not applicable
        if (event.keyCode != KeyEvent.KEYCODE_UNKNOWN) {
            return event
        }

        // Don't offset negative values
        val scanCodeOffset: Int = if (event.scanCode >= 0) {
            InputEventUtils.KEYCODE_TO_SCANCODE_OFFSET
        } else {
            0
        }

        return event.copy(
            // Fallback to scanCode when keyCode is unknown as it's typically more unique
            // Add offset to go past possible keyCode values
            keyCode = event.scanCode + scanCodeOffset,
        )
    }

    private fun logInputEvent(event: KMInputEvent) {
        when (event) {
            is KMEvdevEvent -> {
                Timber.d(
                    "Evdev event: deviceId=${event.deviceId}, type=${event.type}, code=${event.code}, value=${event.value}"
                )
            }

            is KMGamePadEvent -> {
                Timber.d(
                    "GamePad event: deviceId=${event.deviceId}, axisHatX=${event.axisHatX}, axisHatY=${event.axisHatY}"
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
    ) {
        if (clients.containsKey(clientId)) {
            throw IllegalArgumentException("This client already has a callback registered!")
        }

        clients[clientId] = ClientContext(callback, emptySet())
    }

    override fun unregisterClient(clientId: String) {
        clients.remove(clientId)
        invalidateGrabbedEvdevDevices()
    }

    override fun setGrabbedEvdevDevices(clientId: String, deviceDescriptors: List<String>) {
        if (!clients.containsKey(clientId)) {
            throw IllegalArgumentException("This client $clientId is not registered when trying to grab devices!")
        }

        clients[clientId] =
            clients[clientId]!!.copy(grabbedEvdevDevices = deviceDescriptors.toSet())

        invalidateGrabbedEvdevDevices()
    }

    private fun invalidateGrabbedEvdevDevices() {
        val descriptors: Set<String> = clients.values.flatMap { it.grabbedEvdevDevices }.toSet()

        if (systemBridge == null) {
            return
        }

        try {
            systemBridge?.ungrabAllEvdevDevices()

            val inputDevices = devicesAdapter.connectedInputDevices.value.dataOrNull() ?: return

            for (descriptor in descriptors) {
                val device = inputDevices.find { it.descriptor == descriptor } ?: continue

                val grabResult = systemBridge?.grabEvdevDevice(device.id)

                Timber.i("Grabbed evdev device ${device.name}: $grabResult")
            }
        } catch (_: RemoteException) {
            Timber.e("Failed to grab device. Is the system bridge dead?")
        }
    }

    override fun injectEvdevEvent(event: KMEvdevEvent): KMResult<Boolean> {
        val systemBridge = this.systemBridge

        if (systemBridge == null) {
            return SystemBridgeError.Disconnected
        }

        try {
            return systemBridge.writeEvdevEvent(
                event.deviceId,
                event.type,
                event.code,
                event.value
            ).success()
        } catch (e: RemoteException) {
            return KMError.Exception(e)
        }
    }

    override suspend fun injectKeyEvent(event: InjectKeyEventModel): KMResult<Boolean> {
        val systemBridge = this.systemBridge

        if (systemBridge == null) {
            imeInputEventInjector.inputKeyEvent(event)
            return Success(true)
        } else {
            try {
                Timber.d("Injecting input event ${event.keyCode} with system bridge")
                return withContext(Dispatchers.IO) {
                    systemBridge.injectInputEvent(
                        event.toAndroidKeyEvent(),
                        INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH
                    ).success()
                }
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
         * The descriptors of the evdev devices that this client wants to grab.
         */
        val grabbedEvdevDevices: Set<String>
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
    )

    fun unregisterClient(clientId: String)

    /**
     * Set the evdev devices that a client wants to listen to.
     */
    fun setGrabbedEvdevDevices(clientId: String, deviceDescriptors: List<String>)

    /**
     * Inject a key event. This may either use the key event relay service or the system
     * bridge depending on the permissions granted to Key Mapper.
     *
     * Must be suspend so injecting to the systembridge can happen on another thread.
     */
    suspend fun injectKeyEvent(event: InjectKeyEventModel): KMResult<Boolean>

    /**
     * Some callers don't care about the result from injecting and it isn't critical
     * for it to be synchronous so calls to this
     * will be put in a queue and processed.
     */
    fun injectKeyEventAsync(event: InjectKeyEventModel): KMResult<Unit>

    fun injectEvdevEvent(event: KMEvdevEvent): KMResult<Boolean>

    /**
     * Send an input event to the connected clients.
     *
     * @return whether the input event was consumed by a client.
     */
    fun onInputEvent(event: KMInputEvent, detectionSource: InputEventDetectionSource): Boolean

    fun isSystemBridgeConnected(): Boolean
}