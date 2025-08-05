package io.github.sds100.keymapper.sysbridge.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.IContentProvider
import android.ddm.DdmHandleAppName
import android.hardware.input.IInputManager
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ServiceManager
import android.util.Log
import android.view.InputEvent
import io.github.sds100.keymapper.common.utils.getBluetoothAddress
import io.github.sds100.keymapper.common.utils.getDeviceBus
import io.github.sds100.keymapper.sysbridge.IEvdevCallback
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.provider.BinderContainer
import io.github.sds100.keymapper.sysbridge.provider.SystemBridgeBinderProvider
import io.github.sds100.keymapper.sysbridge.utils.IContentProviderUtils
import io.github.sds100.keymapper.sysbridge.utils.InputDeviceIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.DeviceIdleControllerApis
import rikka.hidden.compat.UserManagerApis
import rikka.hidden.compat.adapter.ProcessObserverAdapter
import timber.log.Timber
import kotlin.system.exitProcess

@SuppressLint("LogNotTimber")
internal class SystemBridge : ISystemBridge.Stub() {

    // TODO observe if Key Mapper is uninstalled and stop the process. Look at ApkChangedObservers in Shizuku code.

    // TODO return error code and map this to a SystemBridgeError in key mapper
    external fun grabEvdevDevice(
        deviceIdentifier: InputDeviceIdentifier
    ): Boolean

    external fun ungrabEvdevDevice(deviceId: Int)
    external fun writeEvdevEventNative(deviceId: Int, type: Int, code: Int, value: Int): Boolean

    external fun startEvdevEventLoop(callback: IBinder)
    external fun stopEvdevEventLoop()

    companion object {
        private const val TAG: String = "SystemBridge"

        @JvmStatic
        fun main(args: Array<String>) {
            DdmHandleAppName.setAppName("keymapper_sysbridge", 0)
            @Suppress("DEPRECATION")
            Looper.prepareMainLooper()
            SystemBridge()
            Looper.loop()
        }

        private fun waitSystemService(name: String?) {
            while (ServiceManager.getService(name) == null) {
                try {
                    Log.i(TAG, "service $name is not started, wait 1s.")
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    Log.w(TAG, e.message, e)
                }
            }
        }

        fun sendBinderToApp(
            binder: Binder?,
            packageName: String?,
            userId: Int,
        ) {
            try {
                DeviceIdleControllerApis.addPowerSaveTempWhitelistApp(
                    packageName,
                    30 * 1000,
                    userId,
                    316,  /* PowerExemptionManager#REASON_SHELL */"shell"
                )
                Timber.d(
                    "Add $userId:$packageName to power save temp whitelist for 30s",
                    userId,
                    packageName
                )
            } catch (tr: Throwable) {
                Timber.e(tr)
            }

            val providerName = "$packageName.sysbridge"
            var provider: IContentProvider? = null

            val token: IBinder? = null

            try {
                provider = ActivityManagerApis.getContentProviderExternal(
                    providerName,
                    userId,
                    token,
                    providerName
                )
                if (provider == null) {
                    Log.e(TAG, "provider is null $providerName $userId")
                    return
                }

                if (!provider.asBinder().pingBinder()) {
                    Log.e(TAG, "provider is dead $providerName $userId")
                    return
                }

                val extra = Bundle()
                extra.putParcelable(
                    SystemBridgeBinderProvider.EXTRA_BINDER,
                    BinderContainer(binder)
                )

                val reply: Bundle? = IContentProviderUtils.callCompat(
                    provider,
                    null,
                    providerName,
                    "sendBinder",
                    null,
                    extra
                )
                if (reply != null) {
                    Log.i(TAG, "Send binder to user app $packageName in user $userId")
                } else {
                    Log.w(TAG, "Failed to send binder to user app $packageName in user $userId")
                }
            } catch (tr: Throwable) {
                Log.e(TAG, "Failed to send binder to user app $packageName in user $userId", tr)
            } finally {
                if (provider != null) {
                    try {
                        ActivityManagerApis.removeContentProviderExternal(providerName, token)
                    } catch (tr: Throwable) {
                        Log.w(TAG, "Failed to remove content provider $providerName", tr)
                    }
                }
            }
        }
    }

    private val processObserver = object : ProcessObserverAdapter() {
        override fun onProcessStateChanged(pid: Int, uid: Int, procState: Int) {

        }

        override fun onProcessDied(pid: Int, uid: Int) {

        }
    }

    private val inputManager: IInputManager
    private val coroutineScope: CoroutineScope = MainScope()
    private val mainHandler = Handler(Looper.myLooper()!!)

    private val evdevCallbackLock: Any = Any()
    private var evdevCallback: IEvdevCallback? = null
    private val evdevCallbackDeathRecipient: IBinder.DeathRecipient = IBinder.DeathRecipient {
        Log.d(TAG, "EvdevCallback binder died")
        stopEvdevEventLoop()
    }

    init {
        @SuppressLint("UnsafeDynamicallyLoadedCode")
        System.load("${System.getProperty("keymapper_sysbridge.library.path")}/libevdev.so")
        Log.d(TAG, "SystemBridge started")

        waitSystemService("package")
        waitSystemService(Context.ACTIVITY_SERVICE)
        waitSystemService(Context.USER_SERVICE)
        waitSystemService(Context.APP_OPS_SERVICE)
        waitSystemService(Context.INPUT_SERVICE)

        inputManager =
            IInputManager.Stub.asInterface(ServiceManager.getService(Context.INPUT_SERVICE))

        // TODO check that the key mapper app is installed, otherwise end the process.
//        val ai: ApplicationInfo? = rikka.shizuku.server.ShizukuService.getManagerApplicationInfo()
//        if (ai == null) {
//            System.exit(ServerConstants.MANAGER_APP_NOT_FOUND)
//        }

        // TODO listen for key mapper being uninstalled, and stop the process
//        ApkChangedObservers.start(ai.sourceDir, {
//            if (rikka.shizuku.server.ShizukuService.getManagerApplicationInfo() == null) {
//                LOGGER.w("manager app is uninstalled in user 0, exiting...")
//                System.exit(ServerConstants.MANAGER_APP_NOT_FOUND)
//            }
//        })

        // TODO use the process observer to rebind when key mapper starts


        mainHandler.post {
            for (userId in UserManagerApis.getUserIdsNoThrow()) {
                // TODO use correct package name
                sendBinderToApp(this, "io.github.sds100.keymapper.debug", userId)
            }
        }
    }

    // TODO ungrab all evdev devices
    // TODO ungrab all evdev devices if no key mapper app is bound to the service
    override fun destroy() {
        Log.d(TAG, "SystemBridge destroyed")

        // Must be last line in this method because it halts the JVM.
        exitProcess(0)
    }

    override fun registerEvdevCallback(callback: IEvdevCallback?) {
        callback ?: return

        val binder = callback.asBinder()

        if (this.evdevCallback != null) {
            unregisterEvdevCallback()
        }

        synchronized(evdevCallbackLock) {
            this.evdevCallback = callback
            binder.linkToDeath(evdevCallbackDeathRecipient, 0)
        }

        coroutineScope.launch(Dispatchers.IO) {
            mainHandler.post {
                startEvdevEventLoop(binder)
            }
        }
    }

    override fun unregisterEvdevCallback() {
        synchronized(evdevCallbackLock) {
            evdevCallback?.asBinder()?.unlinkToDeath(evdevCallbackDeathRecipient, 0)
            evdevCallback = null
        }
    }

    override fun grabEvdevDevice(
        deviceId: Int,
    ): Boolean {
        val inputDevice = inputManager.getInputDevice(deviceId)

        val deviceIdentifier = InputDeviceIdentifier(
            id = deviceId,
            name = inputDevice.name,
            vendor = inputDevice.vendorId,
            product = inputDevice.productId,
            descriptor = inputDevice.descriptor,
            bus = inputDevice.getDeviceBus(),
            bluetoothAddress = inputDevice.getBluetoothAddress()
        )

        grabEvdevDevice(deviceIdentifier)

        return true
    }

    override fun injectEvent(event: InputEvent?, mode: Int): Boolean {
        return inputManager.injectInputEvent(event, mode)
    }

    override fun writeEvdevEvent(deviceId: Int, type: Int, code: Int, value: Int): Boolean {
        return writeEvdevEventNative(deviceId, type, code, value)
    }
}