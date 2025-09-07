package io.github.sds100.keymapper.sysbridge.service

import android.annotation.SuppressLint
import android.bluetooth.IBluetoothManager
import android.content.AttributionSource
import android.content.Context
import android.content.IContentProvider
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.hardware.input.IInputManager
import android.net.IConnectivityManager
import android.net.wifi.IWifiManager
import android.nfc.INfcAdapter
import android.nfc.NfcAdapterApis
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.ServiceManager
import android.permission.IPermissionManager
import android.permission.PermissionManagerApis
import android.util.Log
import android.view.InputEvent
import com.android.internal.telephony.ITelephony
import io.github.sds100.keymapper.common.models.EvdevDeviceHandle
import io.github.sds100.keymapper.common.utils.UserHandleUtils
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

        private val systemBridgeVersionCode: Int =
            System.getProperty("keymapper_sysbridge.version_code")!!.toInt()

        private const val KEYMAPPER_CHECK_INTERVAL_MS = 60 * 1000L // 1 minute
        private const val DATA_ENABLED_REASON_USER: Int = 0

        @JvmStatic
        fun main(args: Array<String>) {
            @Suppress("DEPRECATION")
            Looper.prepareMainLooper()
            SystemBridge()
            Looper.loop()
        }

        private fun waitSystemService(name: String?) {
            var count = 0

            while (ServiceManager.getService(name) == null) {
                if (count == 5) {
                    throw IllegalStateException("Failed to get $name system service")
                }

                try {
                    Thread.sleep(1000)
                    count++
                } catch (e: InterruptedException) {
                    Log.w(TAG, e.message, e)
                }
            }
        }
    }

    private val processObserver = object : ProcessObserverAdapter() {

        // This is used as a proxy for detecting the Key Mapper process has started.
        // It is called when ANY foreground activities check so don't execute anything
        // long running.
        override fun onForegroundActivitiesChanged(
            pid: Int,
            uid: Int,
            foregroundActivities: Boolean
        ) {
            if (evdevCallback?.asBinder()?.pingBinder() != true) {
                evdevCallbackDeathRecipient.binderDied()
            }

            // Do not send the binder if the app is not in the foreground.
            if (!foregroundActivities) {
                return
            }

            if (getKeyMapperPackageInfo() == null) {
                Log.i(TAG, "Key Mapper app not installed - exiting")
                destroy()
            } else {
                synchronized(sendBinderLock) {
                    if (evdevCallback == null) {
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

    private val coroutineScope: CoroutineScope = MainScope()
    private val mainHandler = Handler(Looper.myLooper()!!)

    private val keyMapperCheckLock: Any = Any()
    private var keyMapperCheckJob: Job? = null

    private val evdevCallbackLock: Any = Any()
    private var evdevCallback: IEvdevCallback? = null
    private val evdevCallbackDeathRecipient: IBinder.DeathRecipient = IBinder.DeathRecipient {
        Log.i(TAG, "EvdevCallback binder died")
        evdevCallback = null

        coroutineScope.launch(Dispatchers.Default) {
            stopEvdevEventLoop()
        }

        // Start periodic check for Key Mapper installation
        startKeyMapperPeriodicCheck()
    }

    private val inputManager: IInputManager
    private val wifiManager: IWifiManager?
    private val permissionManager: IPermissionManager
    private val telephonyManager: ITelephony?
    private val packageManager: IPackageManager
    private val bluetoothManager: IBluetoothManager?
    private val nfcAdapter: INfcAdapter?
    private val connectivityManager: IConnectivityManager?

    private val processPackageName: String = when (Process.myUid()) {
        Process.ROOT_UID -> "root"
        Process.SHELL_UID -> "com.android.shell"
        else -> throw IllegalStateException("SystemBridge must run as root or shell user")
    }

    init {
        val libraryPath = System.getProperty("keymapper_sysbridge.library.path")
        @SuppressLint("UnsafeDynamicallyLoadedCode")
        System.load("$libraryPath/libevdev.so")

        Log.i(TAG, "SystemBridge starting... Version code $versionCode")

        waitSystemService("package")
        packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))

        waitSystemService(Context.ACTIVITY_SERVICE)
        waitSystemService(Context.USER_SERVICE)
        waitSystemService(Context.APP_OPS_SERVICE)
        waitSystemService("permissionmgr")
        permissionManager =
            IPermissionManager.Stub.asInterface(ServiceManager.getService("permissionmgr"))

        waitSystemService(Context.INPUT_SERVICE)
        inputManager =
            IInputManager.Stub.asInterface(ServiceManager.getService(Context.INPUT_SERVICE))

        if (hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            waitSystemService(Context.WIFI_SERVICE)
            wifiManager =
                IWifiManager.Stub.asInterface(ServiceManager.getService(Context.WIFI_SERVICE))
        } else {
            wifiManager = null
        }

        if (hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            waitSystemService(Context.TELEPHONY_SERVICE)
            telephonyManager =
                ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE))
        } else {
            telephonyManager = null
        }

        if (hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            waitSystemService("bluetooth_manager")
            bluetoothManager =
                IBluetoothManager.Stub.asInterface(ServiceManager.getService("bluetooth_manager"))
        } else {
            bluetoothManager = null
        }

        if (hasSystemFeature(PackageManager.FEATURE_NFC)) {
            waitSystemService(Context.NFC_SERVICE)
            nfcAdapter =
                INfcAdapter.Stub.asInterface(ServiceManager.getService(Context.NFC_SERVICE))
        } else {
            nfcAdapter = null
        }

        waitSystemService(Context.CONNECTIVITY_SERVICE)
        connectivityManager =
            IConnectivityManager.Stub.asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE))

        val applicationInfo = getKeyMapperPackageInfo()

        if (applicationInfo == null) {
            destroy()
        }

        ActivityManagerApis.registerProcessObserver(processObserver)

        // Try sending the binder to the app when its started.
        mainHandler.post {
            sendBinderToApp()
        }

        Log.i(TAG, "SystemBridge started complete. Version code $versionCode")
    }

    private fun hasSystemFeature(name: String): Boolean {
        return packageManager.hasSystemFeature(name, 0)
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

    override fun grabEvdevDevice(devicePath: String?): Boolean {
        devicePath ?: return false
        return grabEvdevDeviceNative(devicePath)
    }

    override fun grabEvdevDeviceArray(devicePath: Array<out String>?): Boolean {
        devicePath ?: return false

        for (path in devicePath) {
            Log.i(TAG, "Grabbing evdev device $path")
            grabEvdevDeviceNative(path)

        }

        return true
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
        if (wifiManager == null) {
            throw UnsupportedOperationException("WiFi not supported")
        }

        return wifiManager.setWifiEnabled(processPackageName, enable)
    }

    override fun writeEvdevEvent(devicePath: String?, type: Int, code: Int, value: Int): Boolean {
        devicePath ?: return false
        return writeEvdevEventNative(devicePath, type, code, value)
    }

    override fun getProcessUid(): Int {
        return Process.myUid()
    }

    override fun grantPermission(permission: String?, deviceId: Int) {
        val userId = UserHandleUtils.getCallingUserId()

        PermissionManagerApis.grantPermission(
            permissionManager,
            systemBridgePackageName ?: return,
            permission ?: return,
            deviceId,
            userId
        )
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

    override fun executeCommand(command: String?): String {
        command ?: throw IllegalArgumentException("command is null")

        Log.i(TAG, "Executing command: $command")

        val process = Runtime.getRuntime().exec(command)

        val out = with(process.inputStream.bufferedReader()) {
            readText()
        }

        val err = with(process.errorStream.bufferedReader()) {
            readText()
        }

        process.waitFor()

        return "$out\n$err"
    }

    override fun getVersionCode(): Int {
        return systemBridgeVersionCode
    }

    override fun setDataEnabled(subId: Int, enable: Boolean) {
        if (telephonyManager == null) {
            throw UnsupportedOperationException("Telephony not supported")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.setDataEnabledForReason(
                subId,
                DATA_ENABLED_REASON_USER,
                enable,
                processPackageName
            )
        } else {
            telephonyManager.setUserDataEnabled(subId, enable)
        }
    }

    override fun setBluetoothEnabled(enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw UnsupportedOperationException("Bluetooth enable/disable requires Android 12 or higher. Otherwise use the SDK's BluetoothAdapter which allows enable/disable.")
        }

        if (bluetoothManager == null) {
            throw UnsupportedOperationException("Bluetooth not supported")
        }

        val attributionSourceBuilder = AttributionSource.Builder(Process.myUid())
            .setAttributionTag("KeyMapperSystemBridge")
            .setPackageName(processPackageName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            attributionSourceBuilder.setPid(Process.myPid())
        }

        val attributionSource = attributionSourceBuilder.build()

        if (enable) {
            bluetoothManager.enable(attributionSource)
        } else {
            bluetoothManager.disable(attributionSource, true)
        }
    }

    override fun setNfcEnabled(enable: Boolean) {
        if (nfcAdapter == null) {
            throw UnsupportedOperationException("NFC not supported")
        }

        if (enable) {
            NfcAdapterApis.enable(nfcAdapter, processPackageName)
        } else {
            NfcAdapterApis.disable(
                adapter = nfcAdapter,
                saveState = true,
                packageName = processPackageName
            )
        }
    }

    override fun setAirplaneMode(enable: Boolean) {
        if (connectivityManager == null) {
            throw UnsupportedOperationException("ConnectivityManager not supported")
        }

        connectivityManager.setAirplaneMode(enable)
    }
}