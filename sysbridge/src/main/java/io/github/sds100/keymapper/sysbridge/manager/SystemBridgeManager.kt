package io.github.sds100.keymapper.sysbridge.manager

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.view.InputDevice
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.sysbridge.IEvdevCallback
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles starting, stopping, (dis)connecting to the system bridge
 */
@Singleton
class SystemBridgeManagerImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope
) : SystemBridgeManager {

    private val inputManager: InputManager by lazy { ctx.getSystemService()!! }

    private val systemBridgeLock: Any = Any()
    private var systemBridge: ISystemBridge? = null

    private val callbackLock: Any = Any()
    private var evdevConnections: ConcurrentHashMap<Int, EvdevConnection> =
        ConcurrentHashMap()
    private val evdevCallback: IEvdevCallback = object : IEvdevCallback.Stub() {
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
    }

    fun pingBinder(): Boolean {
        synchronized(systemBridgeLock) {
            return systemBridge?.asBinder()?.pingBinder() == true
        }
    }

    fun onBinderReceived(binder: IBinder) {
        synchronized(systemBridgeLock) {
            this.systemBridge = ISystemBridge.Stub.asInterface(binder)
            systemBridge?.registerCallback(evdevCallback)
        }
    }

    private fun grabAllDevices() {
        synchronized(callbackLock) {
            val deviceId = 1
            if (evdevConnections.containsKey(deviceId)) {
                removeEvdevConnection(deviceId)
            }

            val connection = EvdevConnection(deviceId, evdevCallback)
            evdevConnections[deviceId] = connection
            evdevCallback.asBinder().linkToDeath(connection, 0)

            return@synchronized evdevCallback
        }

        for (deviceId in inputManager.inputDeviceIds) {
            if (deviceId == -1) {
                continue
            }

            val device = inputManager.getInputDevice(deviceId) ?: continue

            grabInputDevice(device)
        }
    }

    private fun grabInputDevice(inputDevice: InputDevice) {
        val deviceId = inputDevice.id

        try {
            Timber.d("Grabbing input device: ${inputDevice.name} (${inputDevice.id})")

            this.systemBridge?.grabEvdevDevice(deviceId)

            Timber.d("Grabbed input device: ${inputDevice.name} (${inputDevice.id})")


        } catch (e: Exception) {
            Timber.e("Error grabbing input device: ${e.toString()}")
        }

    }

    private fun removeEvdevConnection(deviceId: Int) {
        val connection = evdevConnections.remove(deviceId) ?: return

        // Unlink the death recipient from the connection to remove and
        // delete it from the list of connections for the package.
        connection.callback.asBinder().unlinkToDeath(connection, 0)
    }

    private inner class EvdevConnection(
        private val deviceId: Int,
        val callback: IEvdevCallback,
    ) : DeathRecipient {
        override fun binderDied() {
            Timber.d("EvdevCallback binder died: $deviceId")
            synchronized(callbackLock) {
                removeEvdevConnection(deviceId)
            }
        }
    }

}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.Q)
interface SystemBridgeManager