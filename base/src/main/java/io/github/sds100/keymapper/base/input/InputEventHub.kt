package io.github.sds100.keymapper.base.input

import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.base.BuildConfig
import io.github.sds100.keymapper.base.system.inputmethod.ImeInputEventInjector
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.evdev.IEvdevCallback
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.sysbridge.manager.isConnected
import io.github.sds100.keymapper.system.inputevents.KMEvdevEvent
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.inputevents.KMInputEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class InputEventHubImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val systemBridgeConnManager: SystemBridgeConnectionManager,
    private val imeInputEventInjector: ImeInputEventInjector,
    private val preferenceRepository: PreferenceRepository,
    private val evdevDevicesDelegate: EvdevDevicesDelegate,
) : IEvdevCallback.Stub(),
    InputEventHub {

    companion object {
        const val INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2
    }

    private val clients: ConcurrentHashMap<String, ClientContext> = ConcurrentHashMap()

    // Event queue for processing key events asynchronously in order
    private val keyEventQueue = Channel<InjectKeyEventModel>(capacity = 100)

    private val logInputEventsEnabled: StateFlow<Boolean> =
        preferenceRepository.get(Keys.log).map { isLogEnabled ->
            if (isLogEnabled == true) {
                isLogEnabled
            } else {
                BuildConfig.DEBUG
            }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    init {
        startKeyEventProcessingLoop()

        if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
            coroutineScope.launch {
                systemBridgeConnManager.connectionState
                    .filterIsInstance<SystemBridgeConnectionState.Connected>()
                    .collect {
                        systemBridgeConnManager.run { bridge ->
                            bridge.registerEvdevCallback(this@InputEventHubImpl)
                        }
                    }
            }
        }
    }

    /**
     * Starts a coroutine that processes key events from the queue in order on the IO thread.
     **/
    private fun startKeyEventProcessingLoop() {
        coroutineScope.launch(Dispatchers.IO) {
            for (event in keyEventQueue) {
                try {
                    injectKeyEvent(event, useSystemBridgeIfAvailable = true)
                } catch (e: Exception) {
                    Timber.e(e, "Error processing key event: $event")
                }
            }
        }
    }

    @RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
    override fun isSystemBridgeConnected(): Boolean {
        return systemBridgeConnManager.isConnected()
    }

    @RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
    override fun onEvdevEvent(
        deviceId: Int,
        timeSec: Long,
        timeUsec: Long,
        type: Int,
        code: Int,
        value: Int,
        androidCode: Int,
    ): Boolean {
        val info = evdevDevicesDelegate.getGrabbedDeviceInfo(deviceId) ?: return false
        val evdevEvent =
            KMEvdevEvent(deviceId, info, type, code, value, androidCode, timeSec, timeUsec)

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
                if (!clientContext.evdevEventTypes.contains(event.type) ||
                    clientContext.devicesToGrab.isEmpty()
                ) {
                    continue
                }

                // Only send events from evdev devices to the client if they grabbed it
                if (!clientContext.devicesToGrab.contains(event.deviceInfo)) {
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
                    "Evdev event: deviceId=${event.deviceId}, deviceName=${event.deviceInfo.name}, type=${event.type}, code=${event.code}, value=${event.value}",
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
                        Timber.d(
                            "Key down ${KeyEvent.keyCodeToString(
                                event.keyCode,
                            )}: keyCode=${event.keyCode}, scanCode=${event.scanCode}, deviceId=${event.deviceId}, metaState=${event.metaState}, source=${event.source}",
                        )
                    }

                    KeyEvent.ACTION_UP -> {
                        Timber.d(
                            "Key up ${KeyEvent.keyCodeToString(
                                event.keyCode,
                            )}: keyCode=${event.keyCode}, scanCode=${event.scanCode}, deviceId=${event.deviceId}, metaState=${event.metaState}, source=${event.source}",
                        )
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
        evdevEventTypes: List<Int>,
    ) {
        Timber.d("InputEventHub: Registering client $clientId")
        if (clients.containsKey(clientId)) {
            throw IllegalArgumentException("This client already has a callback registered!")
        }

        clients[clientId] = ClientContext(callback, emptySet(), evdevEventTypes.toSet())
        if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
            invalidateGrabbedDevices()
        }
    }

    override fun unregisterClient(clientId: String) {
        Timber.d("InputEventHub: Unregistering client $clientId")
        clients.remove(clientId)
        if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
            invalidateGrabbedDevices()
        }
    }

    @RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
    override fun setGrabbedEvdevDevices(clientId: String, devices: List<EvdevDeviceInfo>) {
        if (!clients.containsKey(clientId)) {
            throw IllegalArgumentException(
                "This client $clientId is not registered when trying to grab devices!",
            )
        }

        clients[clientId] = clients[clientId]!!.copy(devicesToGrab = devices.toSet())
        invalidateGrabbedDevices()
    }

    @RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
    override fun grabAllEvdevDevices(clientId: String) {
        if (!clients.containsKey(clientId)) {
            throw IllegalArgumentException(
                "This client $clientId is not registered when trying to grab devices!",
            )
        }

        val devices = evdevDevicesDelegate.allDevices.value.toSet()
        clients[clientId] = clients[clientId]!!.copy(devicesToGrab = devices)
        invalidateGrabbedDevices()
    }

    @RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
    override fun injectEvdevEvent(
        deviceId: Int,
        type: Int,
        code: Int,
        value: Int,
    ): KMResult<Boolean> {
        return systemBridgeConnManager.run { bridge ->
            bridge.writeEvdevEvent(
                deviceId,
                type,
                code,
                value,
            )
        }.onSuccess {
            Timber.d("Injected evdev event: $it")
        }
    }

    override suspend fun injectKeyEvent(
        event: InjectKeyEventModel,
        useSystemBridgeIfAvailable: Boolean,
    ): KMResult<Unit> {
        val isSysBridgeConnected = Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API &&
            systemBridgeConnManager.connectionState.value is SystemBridgeConnectionState.Connected

        if (isSysBridgeConnected && useSystemBridgeIfAvailable) {
            val androidKeyEvent = event.toAndroidKeyEvent(flags = KeyEvent.FLAG_FROM_SYSTEM)

            if (logInputEventsEnabled.value) {
                Timber.d("Injecting key event with system bridge $androidKeyEvent")
            }

            return withContext(Dispatchers.IO) {
                // All injected events have their device id set to -1 (VIRTUAL_KEYBOARD_ID)
                // in InputDispatcher.cpp injectInputEvent.
                systemBridgeConnManager.run { bridge ->
                    bridge.injectInputEvent(
                        androidKeyEvent,
                        INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH,
                    )
                }.then { Success(Unit) }
            }
        } else {
            imeInputEventInjector.inputKeyEvent(event)
            return Success(Unit)
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

    override fun onEmergencyKillSystemBridge() {
        preferenceRepository.set(Keys.isSystemBridgeEmergencyKilled, true)
        // Wait for it to be persisted. This method is a synchronous Binder call
        // so the system bridge will not be killed until this returns
        preferenceRepository.get(Keys.isSystemBridgeEmergencyKilled).filter { it == true }
            .firstBlocking()
    }

    @RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
    private fun invalidateGrabbedDevices() {
        val devicesToGrab = clients.values.flatMap { it.devicesToGrab }.toSet()
        evdevDevicesDelegate.setGrabbedDevices(devicesToGrab.toList())
    }

    private data class ClientContext(
        val callback: InputEventHubCallback,
        /**
         * The evdev devices that this client wants to grab.
         */
        val devicesToGrab: Set<EvdevDeviceInfo>,
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
        evdevEventTypes: List<Int>,
    )

    fun unregisterClient(clientId: String)

    fun setGrabbedEvdevDevices(clientId: String, devices: List<EvdevDeviceInfo>)
    fun grabAllEvdevDevices(clientId: String)

    /**
     * Inject a key event. This may either use the key event relay service or the system
     * bridge depending on the permissions granted to Key Mapper.
     *
     * Must be suspend so injecting to the system bridge can happen on another thread.
     */
    suspend fun injectKeyEvent(
        event: InjectKeyEventModel,
        useSystemBridgeIfAvailable: Boolean,
    ): KMResult<Unit>

    /**
     * Some callers don't care about the result from injecting and it isn't critical
     * for it to be synchronous so calls to this
     * will be put in a queue and processed.
     */
    fun injectKeyEventAsync(event: InjectKeyEventModel): KMResult<Unit>

    fun injectEvdevEvent(deviceId: Int, type: Int, code: Int, value: Int): KMResult<Boolean>

    /**
     * Send an input event to the connected clients.
     *
     * @return whether the input event was consumed by a client.
     */
    fun onInputEvent(event: KMInputEvent, detectionSource: InputEventDetectionSource): Boolean

    fun isSystemBridgeConnected(): Boolean
}
