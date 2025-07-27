package io.github.sds100.keymapper.sysbridge.manager

import android.content.Context
import android.hardware.input.InputManager
import android.os.IBinder
import android.view.InputDevice
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.getBluetoothAddress
import io.github.sds100.keymapper.common.utils.getDeviceBus
import io.github.sds100.keymapper.sysbridge.IEvdevCallback
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.utils.InputDeviceIdentifier
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles starting, stopping, (dis)connecting to the system bridge
 */
@Singleton
class SystemBridgeManagerImpl @Inject constructor(
    @ApplicationContext private val ctx: Context
) : SystemBridgeManager {

    private val inputManager: InputManager by lazy { ctx.getSystemService()!! }

    private val lock: Any = Any()
    private var systemBridge: ISystemBridge? = null

    fun pingBinder(): Boolean {
        return false
    }

    fun onBinderReceived(binder: IBinder) {
        synchronized(lock) {
            this.systemBridge = ISystemBridge.Stub.asInterface(binder)

            // TODO remove
            val callback = object : IEvdevCallback.Stub() {
                override fun onEvdevEvent(type: Int, code: Int, value: Int) {
                    Timber.e("Received evdev event: type=$type, code=$code, value=$value")
                }
            }

            val inputDevice: InputDevice = inputManager.getInputDevice(13) ?: return

            val deviceIdentifier = InputDeviceIdentifier(
                name = inputDevice.name,
                vendor = inputDevice.vendorId,
                product = inputDevice.productId,
                descriptor = inputDevice.descriptor,
                bus = inputDevice.getDeviceBus(),
                bluetoothAddress = inputDevice.getBluetoothAddress()
            )

            this.systemBridge?.grabEvdevDevice(deviceIdentifier, callback)
        }
    }
}

interface SystemBridgeManager