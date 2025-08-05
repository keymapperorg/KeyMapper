package io.github.sds100.keymapper.base.input

import android.os.Build
import android.os.RemoteException
import android.view.KeyEvent
import io.github.sds100.keymapper.base.BuildConfig
import io.github.sds100.keymapper.base.system.inputmethod.ImeInputEventInjector
import io.github.sds100.keymapper.common.utils.InputEventType
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.success
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.IEvdevCallback
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnection
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeManager
import io.github.sds100.keymapper.sysbridge.utils.SystemBridgeError
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.inputevents.KMEvdevEvent
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.inputevents.KMInputEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputEventHubImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val systemBridgeManager: SystemBridgeManager,
    private val imeInputEventInjector: ImeInputEventInjector,
    private val preferenceRepository: PreferenceRepository
) : InputEventHub, IEvdevCallback.Stub() {

    companion object {
        private const val INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2
    }

    private val clients: ConcurrentHashMap<String, CallbackContext> =
        ConcurrentHashMap()

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

        return false
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
                    "GamePad event: deviceId=${event.device?.id}, axisHatX=${event.axisHatX}, axisHatY=${event.axisHatY}"
                )
            }

            is KMKeyEvent -> {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        Timber.d("Key down: keyCode=${event.keyCode}, scanCode=${event.scanCode}, deviceId=${event.device?.id}, metaState=${event.metaState}, source=${event.source}")
                    }

                    KeyEvent.ACTION_UP -> {
                        Timber.d("Key up: keyCode=${event.keyCode}, scanCode=${event.scanCode}, deviceId=${event.device?.id}, metaState=${event.metaState}, source=${event.source}")
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
        callback: InputEventHubCallback
    ) {
        if (clients.contains(clientId)) {
            throw IllegalArgumentException("This client already has a callback registered!")
        }

        clients[clientId] = CallbackContext(callback, mutableSetOf())
    }

    override fun unregisterClient(clientId: String) {
        // TODO ungrab the evdev devices
        clients.remove(clientId)
    }

    override fun setCallbackDevices(
        clientId: String,
        deviceIds: Array<Int>
    ) {
    }

    override suspend fun injectEvent(event: KMInputEvent): KMResult<Boolean> {
        when (event) {
            is KMGamePadEvent -> {
                throw IllegalArgumentException("KMGamePadEvents can not be injected. Must use an evdev event instead.")
            }

            is KMKeyEvent -> {
                return injectKeyEvent(event)
            }

            is KMEvdevEvent -> {
                return injectEvdevEvent(event)
            }
        }
    }

    private fun injectEvdevEvent(event: KMEvdevEvent): KMResult<Boolean> {
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

    private suspend fun injectKeyEvent(event: KMKeyEvent): KMResult<Boolean> {
        val systemBridge = this.systemBridge

        if (systemBridge == null) {
            // TODO InputKeyModel will be removed
            val action = if (event.action == KeyEvent.ACTION_DOWN) {
                InputEventType.DOWN
            } else {
                InputEventType.UP
            }

            val model = InputKeyModel(
                keyCode = event.keyCode,
                inputType = action,
                metaState = event.metaState,
                deviceId = event.device?.id ?: -1,
                scanCode = event.scanCode,
                repeat = event.repeatCount,
                source = event.source
            )

            imeInputEventInjector.inputKeyEvent(model)

            return Success(true)
        } else {
            try {
                return systemBridge.injectEvent(
                    event.toKeyEvent(),
                    INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH
                ).success()
            } catch (e: RemoteException) {
                return KMError.Exception(e)
            }
        }
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
    fun setCallbackDevices(clientId: String, deviceIds: Array<Int>)

    /**
     * Inject an input event. This may either use the key event relay service or the system
     * bridge depending on the permissions granted to Key Mapper.
     */
    suspend fun injectEvent(event: KMInputEvent): KMResult<Boolean>

    /**
     * Send an input event to the connected clients.
     *
     * @return whether the input event was consumed by a client.
     */
    fun onInputEvent(event: KMInputEvent, detectionSource: InputEventDetectionSource): Boolean
}