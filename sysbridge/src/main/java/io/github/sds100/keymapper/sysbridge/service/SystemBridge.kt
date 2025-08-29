package io.github.sds100.keymapper.sysbridge.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.IContentProvider
import android.content.pm.ApplicationInfo
import android.ddm.DdmHandleAppName
import android.hardware.input.IInputManager
import android.net.wifi.IWifiManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.ServiceManager
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.view.InputEvent
import androidx.core.os.bundleOf
import io.github.sds100.keymapper.common.models.EvdevDeviceHandle
import io.github.sds100.keymapper.sysbridge.IEvdevCallback
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.provider.BinderContainer
import io.github.sds100.keymapper.sysbridge.provider.SystemBridgeBinderProvider
import io.github.sds100.keymapper.sysbridge.utils.IContentProviderUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.DeviceIdleControllerApis
import rikka.hidden.compat.PackageManagerApis
import rikka.hidden.compat.UserManagerApis
import rikka.hidden.compat.adapter.ProcessObserverAdapter
import kotlin.system.exitProcess


@SuppressLint("LogNotTimber")
internal class SystemBridge : ISystemBridge.Stub() {

    external fun grabEvdevDeviceNative(devicePath: String): Boolean

    external fun ungrabEvdevDeviceNative(devicePath: String): Boolean
    external fun ungrabAllEvdevDevicesNative(): Boolean
    external fun writeEvdevEventNative(
        devicePath: String,
        type: Int,
        code: Int,
        value: Int
    ): Boolean

    external fun getEvdevDevicesNative(): Array<EvdevDeviceHandle>

    external fun startEvdevEventLoop(callback: IBinder)
    external fun stopEvdevEventLoop()

    companion object {
        private const val TAG: String = "KeyMapperSystemBridge"
        private val systemBridgePackageName: String? =
            System.getProperty("keymapper_sysbridge.package")
        private const val SHELL_PACKAGE = "com.android.shell"

        private const val KEYMAPPER_CHECK_INTERVAL_MS = 60 * 1000L // 1 minute

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
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    Log.w(TAG, e.message, e)
                }
            }
        }
    }

    private val processObserver = object : ProcessObserverAdapter() {

        // This is used as a proxy for detecting the Key Mapper process has started.
        override fun onForegroundActivitiesChanged(
            pid: Int,
            uid: Int,
            foregroundActivities: Boolean
        ) {
            // Do not send the binder if the app is not in the foreground.
            if (!foregroundActivities) {
                return
            }

            if (getKeyMapperPackageInfo() == null) {
                Log.i(TAG, "Key Mapper app not installed - exiting")
                destroy()
            } else {
                synchronized(sendBinderLock) {
                    if (!isBinderSent) {
                        Log.i(TAG, "Key Mapper process started, send binder to app")
                        mainHandler.post {
                            sendBinderToApp()
                        }
                    }
                }
            }
        }
    }

    private val sendBinderLock: Any = Any()
    private var isBinderSent: Boolean = false

    private val coroutineScope: CoroutineScope = MainScope()
    private val mainHandler = Handler(Looper.myLooper()!!)

    private val keyMapperCheckLock: Any = Any()
    private var keyMapperCheckJob: Job? = null

    private val evdevCallbackLock: Any = Any()
    private var evdevCallback: IEvdevCallback? = null
    private val evdevCallbackDeathRecipient: IBinder.DeathRecipient = IBinder.DeathRecipient {
        Log.i(TAG, "EvdevCallback binder died")

        synchronized(sendBinderLock) {
            isBinderSent = false
        }

        coroutineScope.launch(Dispatchers.Default) {
            stopEvdevEventLoop()
        }

        // Start periodic check for Key Mapper installation
        startKeyMapperPeriodicCheck()
    }

    private val inputManager: IInputManager
    private val wifiManager: IWifiManager

    private val processPackageName = if (Process.myUid() == Process.ROOT_UID) {
        "root"
    } else {
        "com.android.shell"
    }

    init {
        val libraryPath = System.getProperty("keymapper_sysbridge.library.path")
        @SuppressLint("UnsafeDynamicallyLoadedCode")
        System.load("$libraryPath/libevdev.so")

        Log.i(TAG, "SystemBridge started")

        waitSystemService("package")
        waitSystemService(Context.ACTIVITY_SERVICE)
        waitSystemService(Context.USER_SERVICE)
        waitSystemService(Context.APP_OPS_SERVICE)

        waitSystemService(Context.INPUT_SERVICE)
        inputManager =
            IInputManager.Stub.asInterface(ServiceManager.getService(Context.INPUT_SERVICE))

        waitSystemService(Context.WIFI_SERVICE)
        wifiManager =
            IWifiManager.Stub.asInterface(ServiceManager.getService(Context.WIFI_SERVICE))

        val applicationInfo = getKeyMapperPackageInfo()

        if (applicationInfo == null) {
            destroy()
        }

        ActivityManagerApis.registerProcessObserver(processObserver)

        // Try sending the binder to the app when its started.
        mainHandler.post {
            sendBinderToApp()
        }
    }

    private fun getKeyMapperPackageInfo(): ApplicationInfo? =
        PackageManagerApis.getApplicationInfoNoThrow(systemBridgePackageName, 0, 0)

    private fun startKeyMapperPeriodicCheck() {
        synchronized(keyMapperCheckLock) {
            keyMapperCheckJob?.cancel()

            Log.i(TAG, "Starting periodic Key Mapper installation check")

            keyMapperCheckJob = coroutineScope.launch(Dispatchers.Default) {
                try {
                    while (true) {
                        if (getKeyMapperPackageInfo() == null) {
                            Log.i(TAG, "Key Mapper not installed - exiting")
                            destroy()
                            break
                        } else {
                            // While Key Mapper is still installed but not bound, then periodically
                            // check if it has uninstalled
                            delay(KEYMAPPER_CHECK_INTERVAL_MS)
                        }
                    }
                } finally {
                    // Clear the job reference when the coroutine completes
                    synchronized(keyMapperCheckLock) {
                        if (keyMapperCheckJob?.isCompleted == true) {
                            keyMapperCheckJob = null
                        }
                    }
                }
            }
        }
    }

    private fun stopKeyMapperPeriodicCheck() {
        synchronized(keyMapperCheckLock) {
            keyMapperCheckJob?.cancel()
            keyMapperCheckJob = null
            Log.i(TAG, "Stopped periodic Key Mapper installation check")
        }
    }

    override fun destroy() {
        Log.i(TAG, "SystemBridge destroyed")

        // Clean up periodic check job
        stopKeyMapperPeriodicCheck()

        // Must be last line in this method because it halts the JVM.
        exitProcess(0)
    }

    override fun registerEvdevCallback(callback: IEvdevCallback?) {
        callback ?: return

        Log.i(TAG, "Register evdev callback")

        // Stop periodic check since Key Mapper has reconnected
        stopKeyMapperPeriodicCheck()

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
            stopEvdevEventLoop()
        }
    }

    // TODO passthrough an optional timeout that will automatically ungrab after that time. Use this when recording.
    override fun grabEvdevDevice(devicePath: String?): Boolean {
        devicePath ?: return false
        return grabEvdevDeviceNative(devicePath)
    }

    override fun ungrabEvdevDevice(devicePath: String?): Boolean {
        devicePath ?: return false
        ungrabEvdevDeviceNative(devicePath)
        return true
    }

    override fun ungrabAllEvdevDevices(): Boolean {
        ungrabAllEvdevDevicesNative()
        return true
    }

    override fun injectInputEvent(event: InputEvent?, mode: Int): Boolean {
        return inputManager.injectInputEvent(event, mode)
    }

    override fun getEvdevInputDevices(): Array<out EvdevDeviceHandle?>? {
        return getEvdevDevicesNative()
    }

    override fun setWifiEnabled(enable: Boolean): Boolean {
        return wifiManager.setWifiEnabled(SHELL_PACKAGE, enable)
    }

    override fun writeEvdevEvent(devicePath: String?, type: Int, code: Int, value: Int): Boolean {
        devicePath ?: return false
        return writeEvdevEventNative(devicePath, type, code, value)
    }

    // TODO If Key Mapper has WRITE_SECURE_SETTINGS permission it can write to Global settings itself.
    // Replace with a method to request permissions for Key Mapper. If Key Mapper does not have WRITE_SECURE_SETTINGS permission
    // then the action will show an error that it needs WRITE_SECURE_SETTINGS. The action will explain that Key Mapper can grant itself if they start PRO Mode one-time.
    // Everytime PRO mode is started, Key Mapper should grant itself WRITE_SECURE_SETTINGS
    override fun putGlobalSetting(name: String, value: String) {
        val providerName = "settings"

        val token: IBinder? = null
        val userId: Int = UserHandle::class.java.getMethod("getCallingUserId").invoke(null) as Int

        Log.d(TAG, "Putting global setting $name = $value for user $userId")

        val settingsProvider = ActivityManagerApis.getContentProviderExternal(
            providerName,
            userId,
            token,
            providerName
        )

        if (settingsProvider == null) {
            Log.w(TAG, "Failed to get settings provider")
            return
        }

        val bundle = bundleOf(
            Settings.NameValueTable.VALUE to value
        )

        IContentProviderUtils.callCompat(
            settingsProvider,
            processPackageName,
            providerName,
            "PUT_global",
            name,
            bundle
        )

        Log.i(TAG, "Put global setting $name = $value")
    }

    private fun sendBinderToApp(): Boolean {
        // Only support Key Mapper running in a single Android user for now so just send
        // it to the first user that accepts the binder.
        for (userId in UserManagerApis.getUserIdsNoThrow()) {
            if (sendBinderToAppInUser(userId)) {
                return true
            }
        }

        return false
    }

    /**
     * @return Whether it was sent successfully with a reply from the app.
     */
    private fun sendBinderToAppInUser(userId: Int): Boolean {
        try {
            DeviceIdleControllerApis.addPowerSaveTempWhitelistApp(
                systemBridgePackageName,
                30 * 1000,
                userId,
                316,  /* PowerExemptionManager#REASON_SHELL */"shell"
            )
        } catch (tr: Throwable) {
            Log.e(TAG, tr.toString())
        }

        val providerName = "$systemBridgePackageName.sysbridge"
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
                return false
            }

            if (!provider.asBinder().pingBinder()) {
                Log.e(TAG, "provider is dead $providerName $userId")
                return false
            }

            val extra = Bundle()
            extra.putParcelable(
                SystemBridgeBinderProvider.EXTRA_BINDER,
                BinderContainer(this)
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
                Log.i(TAG, "Send binder to user app $systemBridgePackageName in user $userId")
                isBinderSent = true
                // Stop periodic check since connection is successful
                stopKeyMapperPeriodicCheck()
                return true
            } else {
                Log.w(
                    TAG,
                    "Failed to send binder to user app $systemBridgePackageName in user $userId"
                )
            }
        } catch (tr: Throwable) {
            Log.e(
                TAG,
                "Failed to send binder to user app $systemBridgePackageName in user $userId",
                tr
            )
        } finally {
            if (provider != null) {
                try {
                    ActivityManagerApis.removeContentProviderExternal(providerName, token)
                } catch (tr: Throwable) {
                    Log.w(TAG, "Failed to remove content provider $providerName", tr)
                }
            }
        }

        return false
    }
}