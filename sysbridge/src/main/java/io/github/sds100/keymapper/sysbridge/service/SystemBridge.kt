package io.github.sds100.keymapper.sysbridge.service

import android.annotation.SuppressLint
import android.app.ActivityTaskManagerApis
import android.app.IActivityManager
import android.app.IActivityTaskManager
import android.bluetooth.IBluetoothManager
import android.content.AttributionSource
import android.content.Context
import android.content.IContentProvider
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.hardware.input.IInputManager
import android.hardware.usb.IUsbManager
import android.media.IAudioService
import android.net.IConnectivityManager
import android.net.ITetheringConnector
import android.net.ITetheringEventCallback
import android.net.Network
import android.net.TetherStatesParcel
import android.net.TetheredClient
import android.net.TetheringCallbackStartedParcel
import android.net.TetheringConfigurationParcel
import android.net.TetheringRequestParcel
import android.net.wifi.IWifiManager
import android.nfc.INfcAdapter
import android.nfc.NfcAdapterApis
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.RemoteException
import android.os.ServiceManager
import android.permission.IPermissionManager
import android.permission.PermissionManagerApis
import android.util.Log
import android.view.InputEvent
import androidx.annotation.RequiresApi
import com.android.internal.telephony.ITelephony
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.models.GrabTargetKeyCode
import io.github.sds100.keymapper.common.models.GrabbedDeviceHandle
import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.UserHandleUtils
import io.github.sds100.keymapper.evdev.IEvdevCallback
import io.github.sds100.keymapper.sysbridge.ILogCallback
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.provider.BinderContainer
import io.github.sds100.keymapper.sysbridge.provider.SystemBridgeBinderProvider
import io.github.sds100.keymapper.sysbridge.utils.IContentProviderUtils
import java.io.InterruptedIOException
import kotlin.system.exitProcess
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

@SuppressLint("LogNotTimber")
class SystemBridge : ISystemBridge.Stub() {

    @Suppress("KotlinJniMissingFunction")
    external fun setGrabTargetsNative(
        devices: Array<GrabTargetKeyCode>,
    ): Array<GrabbedDeviceHandle>

    @Suppress("KotlinJniMissingFunction")
    external fun writeEvdevEventNative(deviceId: Int, type: Int, code: Int, value: Int): Boolean

    @Suppress("KotlinJniMissingFunction")
    external fun writeEvdevEventKeyCodeNative(deviceId: Int, keyCode: Int, value: Int): Boolean

    @Suppress("KotlinJniMissingFunction")
    external fun getEvdevDevicesNative(): Array<EvdevDeviceInfo>

    @Suppress("KotlinJniMissingFunction")
    external fun initEvdevManager()

    @Suppress("KotlinJniMissingFunction")
    external fun destroyEvdevManager()

    @Suppress("KotlinJniMissingFunction")
    external fun setLogLevelNative(level: Int)

    /**
     * Called from Rust via JNI when an evdev event occurs.
     * Forwards the call to the registered IEvdevCallback and returns whether the event was consumed.
     */
    @Suppress("unused")
    fun onEvdevEvent(
        deviceId: Int,
        timeSec: Long,
        timeUsec: Long,
        type: Int,
        code: Int,
        value: Int,
        androidCode: Int,
    ): Boolean {
        synchronized(evdevCallbackLock) {
            val callback = evdevCallback ?: return false
            return try {
                callback.onEvdevEvent(deviceId, timeSec, timeUsec, type, code, value, androidCode)
            } catch (e: Exception) {
                Log.e(TAG, "Error calling evdev callback", e)
                false
            }
        }
    }

    @Suppress("unused")
    fun onGrabbedDevicesChanged(devices: Array<GrabbedDeviceHandle>) {
        synchronized(evdevCallbackLock) {
            val callback = evdevCallback ?: return
            try {
                callback.onGrabbedDevicesChanged(devices)
            } catch (e: Exception) {
                Log.e(TAG, "Error calling evdev callback", e)
            }
        }
    }

    @Suppress("unused")
    fun onEvdevDevicesChanged(devices: Array<EvdevDeviceInfo>) {
        synchronized(evdevCallbackLock) {
            val callback = evdevCallback ?: return
            try {
                callback.onEvdevDevicesChanged(devices)
            } catch (e: Exception) {
                Log.e(TAG, "Error calling evdev callback", e)
            }
        }
    }

    /**
     * Called from Rust via JNI when the power button is held for 10+ seconds.
     * Forwards the call to the registered IEvdevCallback for emergency system bridge kill.
     */
    @Suppress("unused")
    fun onEmergencyKillSystemBridge() {
        synchronized(evdevCallbackLock) {
            evdevCallback?.onEmergencyKillSystemBridge()
        }
    }

    /**
     * Called from Rust via JNI when a log message is emitted.
     * Forwards the call to the registered ILogCallback.
     */
    @Suppress("unused")
    fun onLogMessage(level: Int, message: String) {
        synchronized(logCallbackLock) {
            val callback = logCallback ?: return
            try {
                callback.onLog(level, message)
            } catch (e: Exception) {
                Log.e(TAG, "Error calling log callback", e)
            }
        }
    }

    companion object {
        private const val TAG: String = "KeyMapperSystemBridge"
        private val systemBridgePackageName: String? by lazy {
            System.getProperty("keymapper_sysbridge.package")
        }

        private val systemBridgeVersionCode: Int by lazy {
            System.getProperty("keymapper_sysbridge.version")?.toIntOrNull() ?: -1
        }

        private const val KEYMAPPER_CHECK_INTERVAL_MS = 60 * 1000L // 1 minute
        private const val DATA_ENABLED_REASON_USER: Int = 0
        private const val TETHERING_WIFI: Int = 0

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
            foregroundActivities: Boolean,
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
    private val mainHandler by lazy { Handler(Looper.myLooper()!!) }

    private val keyMapperCheckLock: Any = Any()
    private var keyMapperCheckJob: Job? = null

    private val evdevCallbackLock: Any = Any()
    private var evdevCallback: IEvdevCallback? = null
    private val evdevCallbackDeathRecipient: IBinder.DeathRecipient = IBinder.DeathRecipient {
        Log.i(TAG, "EvdevCallback binder died. Stopping evdev event loop")

        synchronized(evdevCallbackLock) {
            evdevCallback = null
        }

        // Start periodic check for Key Mapper installation
        startKeyMapperPeriodicCheck()
    }

    private val logCallbackLock: Any = Any()
    private var logCallback: ILogCallback? = null
    private val logCallbackDeathRecipient: IBinder.DeathRecipient = IBinder.DeathRecipient {
        synchronized(logCallbackLock) {
            logCallback = null
        }
    }

    private val inputManager: IInputManager by lazy {
        waitSystemService(Context.INPUT_SERVICE)
        IInputManager.Stub.asInterface(ServiceManager.getService(Context.INPUT_SERVICE))
    }

    private val wifiManager: IWifiManager? by lazy {
        if (hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            waitSystemService(Context.WIFI_SERVICE)
            IWifiManager.Stub.asInterface(ServiceManager.getService(Context.WIFI_SERVICE))
        } else {
            null
        }
    }

    private val permissionManager: IPermissionManager? by lazy {
        waitSystemService("permissionmgr")
        IPermissionManager.Stub.asInterface(ServiceManager.getService("permissionmgr"))
    }

    private val telephonyManager: ITelephony? by lazy {
        if (hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            waitSystemService(Context.TELEPHONY_SERVICE)
            ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE))
        } else {
            null
        }
    }

    private val packageManager: IPackageManager by lazy {
        waitSystemService("package")
        IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    }

    private val bluetoothManager: IBluetoothManager? by lazy {
        if (hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            waitSystemService("bluetooth_manager")
            IBluetoothManager.Stub.asInterface(ServiceManager.getService("bluetooth_manager"))
        } else {
            null
        }
    }

    private val nfcAdapter: INfcAdapter? by lazy {
        if (hasSystemFeature(PackageManager.FEATURE_NFC)) {
            waitSystemService(Context.NFC_SERVICE)
            INfcAdapter.Stub.asInterface(ServiceManager.getService(Context.NFC_SERVICE))
        } else {
            null
        }
    }

    private val connectivityManager: IConnectivityManager? by lazy {

        waitSystemService(Context.CONNECTIVITY_SERVICE)
        IConnectivityManager.Stub.asInterface(
            ServiceManager.getService(Context.CONNECTIVITY_SERVICE),
        )
    }
    private val tetheringConnector: ITetheringConnector? by lazy {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            waitSystemService("tethering")
            ITetheringConnector.Stub.asInterface(ServiceManager.getService("tethering"))
        } else {
            null
        }
    }
    private val activityManager: IActivityManager
    private val activityTaskManager: IActivityTaskManager by lazy {
        waitSystemService("activity_task")
        IActivityTaskManager.Stub.asInterface(
            ServiceManager.getService("activity_task"),
        )
    }

    private val audioService: IAudioService? by lazy {
        waitSystemService(Context.AUDIO_SERVICE)
        IAudioService.Stub.asInterface(ServiceManager.getService(Context.AUDIO_SERVICE))
    }

    private val usbManager: IUsbManager? by lazy {
        waitSystemService(Context.USB_SERVICE)
        IUsbManager.Stub.asInterface(ServiceManager.getService(Context.USB_SERVICE))
    }

    private val processPackageName: String = when (Process.myUid()) {
        Process.ROOT_UID -> "root"
        Process.SHELL_UID -> "com.android.shell"
        else -> throw IllegalStateException("SystemBridge must run as root or shell user")
    }

    init {
        if (versionCode == -1) {
            Log.e(TAG, "SystemBridge version code not set")
            throw IllegalStateException("SystemBridge version code not set")
        }

        val libraryPath = System.getProperty("keymapper_sysbridge.library.path")
        @SuppressLint("UnsafeDynamicallyLoadedCode")
        System.load("$libraryPath/libevdev_manager.so")

        Log.i(TAG, "SystemBridge starting... Version code $versionCode")

        waitSystemService(Context.ACTIVITY_SERVICE)
        activityManager = IActivityManager.Stub.asInterface(
            ServiceManager.getService(Context.ACTIVITY_SERVICE),
        )

        val applicationInfo = getKeyMapperPackageInfo()

        if (applicationInfo == null) {
            destroy()
        }

        ActivityManagerApis.registerProcessObserver(processObserver)

        // Try sending the binder to the app when its started.
        mainHandler.post {
            sendBinderToApp()
        }

        initEvdevManager()

        waitSystemService(Context.USER_SERVICE)
        waitSystemService(Context.APP_OPS_SERVICE)

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
        Log.i(TAG, "Destroying system bridge...")

        stopKeyMapperPeriodicCheck()
        destroyEvdevManager()

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
    }

    override fun unregisterEvdevCallback() {
        synchronized(evdevCallbackLock) {
            evdevCallback?.asBinder()?.unlinkToDeath(evdevCallbackDeathRecipient, 0)
            evdevCallback = null
        }
    }

    override fun setGrabTargets(
        devices: Array<out GrabTargetKeyCode?>?,
    ): Array<out GrabbedDeviceHandle?>? {
        return setGrabTargetsNative(devices?.filterNotNull()?.toTypedArray() ?: emptyArray())
    }

    override fun injectInputEvent(event: InputEvent?, mode: Int): Boolean {
        try {
            return inputManager.injectInputEvent(event, mode)
        } catch (e: SecurityException) {
            logCallback?.onLog(Log.WARN, "Failed to inject event due to security exception: $e")
            return false
        }
    }

    override fun getEvdevInputDevices(): Array<out EvdevDeviceInfo?> {
        return getEvdevDevicesNative()
    }

    override fun setWifiEnabled(enable: Boolean): Boolean {
        if (wifiManager == null) {
            throw UnsupportedOperationException("WiFi not supported")
        }

        return wifiManager!!.setWifiEnabled(processPackageName, enable)
    }

    override fun writeEvdevEvent(deviceId: Int, type: Int, code: Int, value: Int): Boolean {
        return writeEvdevEventNative(deviceId, type, code, value)
    }

    override fun writeEvdevEventKeyCode(deviceId: Int, keyCode: Int, value: Int): Boolean {
        return writeEvdevEventKeyCodeNative(deviceId, keyCode, value)
    }

    override fun getProcessUid(): Int {
        return Process.myUid()
    }

    override fun grantPermission(permission: String?, deviceId: Int) {
        val userId = UserHandleUtils.getCallingUserId()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            PermissionManagerApis.grantPermission(
                permissionManager!!,
                systemBridgePackageName ?: return,
                permission ?: return,
                deviceId,
                userId,
            )
        } else {
            PermissionManagerApis.grantPermission(
                packageManager,
                systemBridgePackageName ?: return,
                permission ?: return,
                userId,
            )
        }
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
                // PowerExemptionManager#REASON_SHELL
                316,
                "shell",
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
                providerName,
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
                BinderContainer(this),
            )

            val reply: Bundle? = IContentProviderUtils.callCompat(
                provider,
                null,
                providerName,
                "sendBinder",
                null,
                extra,
            )
            if (reply != null) {
                Log.i(TAG, "Send binder to user app $systemBridgePackageName in user $userId")
                // Stop periodic check since connection is successful
                stopKeyMapperPeriodicCheck()
                return true
            } else {
                Log.w(
                    TAG,
                    "Failed to send binder to user app $systemBridgePackageName in user $userId",
                )
            }
        } catch (tr: Throwable) {
            Log.e(
                TAG,
                "Failed to send binder to user app $systemBridgePackageName in user $userId",
                tr,
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

    override fun executeCommand(command: String?, timeoutMillis: Long): ShellResult {
        command ?: throw IllegalArgumentException("Command is null")

        val process = ProcessBuilder()
            .command("sh", "-c", command)
            // Redirect stderr to stdout
            .redirectErrorStream(true)
            .start()

        var stdout = ""

        val worker = Thread {
            val stdoutReader = process.inputStream.bufferedReader()

            try {
                stdout = stdoutReader.readText()
                process.waitFor()
            } catch (_: InterruptedException) {
            } catch (_: InterruptedIOException) {
            } finally {
                stdoutReader.close()
            }
        }

        worker.start()

        try {
            worker.join(timeoutMillis)

            if (worker.isAlive) {
                worker.interrupt()
                process.destroy()
                // Only some standard exceptions can be thrown across Binder. A TimeoutException
                // is not one of them.
                throw IllegalStateException("Timeout")
            }
        } catch (e: InterruptedException) {
            worker.interrupt()
            Thread.currentThread().interrupt()
        }

        val exitCode = process.exitValue()

        return ShellResult(stdout, exitCode)
    }

    override fun getVersionCode(): Int {
        return systemBridgeVersionCode
    }

    override fun setDataEnabled(subId: Int, enable: Boolean) {
        if (telephonyManager == null) {
            throw UnsupportedOperationException("Telephony not supported")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager!!.setDataEnabledForReason(
                subId,
                DATA_ENABLED_REASON_USER,
                enable,
                processPackageName,
            )
        } else {
            telephonyManager!!.setUserDataEnabled(subId, enable)
        }
    }

    override fun setBluetoothEnabled(enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw UnsupportedOperationException(
                "Bluetooth enable/disable requires Android 12 or higher. Otherwise use the SDK's BluetoothAdapter which allows enable/disable.",
            )
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
            bluetoothManager!!.enable(attributionSource)
        } else {
            bluetoothManager!!.disable(attributionSource, true)
        }
    }

    override fun setNfcEnabled(enable: Boolean) {
        if (nfcAdapter == null) {
            throw UnsupportedOperationException("NFC not supported")
        }

        if (enable) {
            NfcAdapterApis.enable(nfcAdapter!!, processPackageName)
        } else {
            NfcAdapterApis.disable(
                adapter = nfcAdapter!!,
                saveState = true,
                packageName = processPackageName,
            )
        }
    }

    override fun setAirplaneMode(enable: Boolean) {
        if (connectivityManager == null) {
            throw UnsupportedOperationException("ConnectivityManager not supported")
        }

        connectivityManager!!.setAirplaneMode(enable)
    }

    override fun forceStopPackage(packageName: String?) {
        val userId = UserHandleUtils.getCallingUserId()

        activityManager.forceStopPackage(packageName, userId)
    }

    override fun removeTasks(packageName: String?) {
        packageName ?: return

        val tasks =
            ActivityTaskManagerApis.getTasks(
                activityTaskManager = activityTaskManager,
                maxNum = 32,
                filterOnlyVisibleRecents = false,
                keepIntentExtra = false,
                displayId = 0,
            ) ?: return

        tasks.filterNotNull()
            .filter { it.baseActivity?.packageName == packageName }
            .forEach { activityManager.removeTask(it.taskId) }
    }

    override fun setRingerMode(ringerMode: Int) {
        if (audioService == null) {
            throw UnsupportedOperationException("AudioService not supported")
        }

        audioService!!.setRingerModeInternal(ringerMode, processPackageName)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun isTetheringEnabled(): Boolean {
        if (tetheringConnector == null) {
            throw UnsupportedOperationException("TetheringConnector not supported")
        }

        val lock = Object()
        var result = false
        val timeoutMillis = 5000L

        val callback = object : ITetheringEventCallback.Stub() {
            override fun onCallbackStarted(parcel: TetheringCallbackStartedParcel?) {
                if (parcel?.states?.tetheredList != null) {
                    // Check if any tethering interface is active
                    result = parcel.states.tetheredList.isNotEmpty()
                }

                synchronized(lock) {
                    lock.notify()
                }
            }

            override fun onCallbackStopped(errorCode: Int) {}
            override fun onUpstreamChanged(network: Network?) {}
            override fun onConfigurationChanged(config: TetheringConfigurationParcel?) {}
            override fun onTetherStatesChanged(states: TetherStatesParcel?) {}
            override fun onTetherClientsChanged(clients: List<TetheredClient?>?) {}
            override fun onOffloadStatusChanged(status: Int) {}
            override fun onSupportedTetheringTypes(supportedBitmap: Long) {}
        }

        try {
            // We register and immediately unregister the callback after getting the state
            // instead of keeping it registered for the lifetime of SystemBridge. This is
            // a safety measure in case there's a bug in the callback that could crash
            // the entire SystemBridge process.
            tetheringConnector!!.registerTetheringEventCallback(callback, processPackageName)

            // Wait for callback with timeout using Handler
            mainHandler.postDelayed(
                {
                    synchronized(lock) {
                        lock.notify()
                    }
                },
                timeoutMillis,
            )

            synchronized(lock) {
                lock.wait(timeoutMillis)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            tetheringConnector!!.unregisterTetheringEventCallback(callback, processPackageName)
        }

        return result
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun setTetheringEnabled(enable: Boolean) {
        if (tetheringConnector == null) {
            throw UnsupportedOperationException("TetheringConnector not supported")
        }

        if (enable) {
            val request = TetheringRequestParcel().apply {
                // TetheringManager.TETHERING_WIFI
                tetheringType = TETHERING_WIFI
                localIPv4Address = null
                staticClientAddress = null
                exemptFromEntitlementCheck = false
                showProvisioningUi = true
                // TetheringManager.CONNECTIVITY_SCOPE_GLOBAL
                connectivityScope = 1
            }

            tetheringConnector!!.startTethering(request, processPackageName, null, null)
        } else {
            tetheringConnector!!.stopTethering(TETHERING_WIFI, processPackageName, null, null)
        }
    }

    override fun getUsbScreenUnlockedFunctions(): Long {
        return try {
            usbManager?.screenUnlockedFunctions ?: 0
        } catch (_: RemoteException) {
            -1
        }
    }

    override fun registerLogCallback(callback: ILogCallback?) {
        callback ?: return

        Log.i(TAG, "Register log callback")

        val binder = callback.asBinder()

        if (this.logCallback != null) {
            unregisterLogCallback()
        }

        synchronized(logCallbackLock) {
            this.logCallback = callback
            binder.linkToDeath(logCallbackDeathRecipient, 0)
        }
    }

    override fun unregisterLogCallback() {
        synchronized(logCallbackLock) {
            logCallback?.asBinder()?.unlinkToDeath(logCallbackDeathRecipient, 0)
            logCallback = null
        }
    }

    override fun setLogLevel(level: Int) {
        setLogLevelNative(level)
    }
}
