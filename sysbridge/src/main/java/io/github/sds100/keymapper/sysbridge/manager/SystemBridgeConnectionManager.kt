package io.github.sds100.keymapper.sysbridge.manager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.os.Process
import android.os.RemoteException
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.starter.SystemBridgeStarter
import io.github.sds100.keymapper.sysbridge.utils.SystemBridgeError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles starting, stopping and (dis)connecting to the system bridge.
 */
@Singleton
class SystemBridgeConnectionManagerImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope,
    private val starter: SystemBridgeStarter,
) : SystemBridgeConnectionManager {

    private val systemBridgeLock: Any = Any()
    private var systemBridgeFlow: MutableStateFlow<ISystemBridge?> = MutableStateFlow(null)

    override val isConnected: Flow<Boolean> = systemBridgeFlow.map { it != null }
    override val onUnexpectedDeath: Channel<Unit> = Channel()
    private var isExpectedDeath: Boolean = false

    private val deathRecipient: DeathRecipient = DeathRecipient {
        synchronized(systemBridgeLock) {
            systemBridgeFlow.update { null }

            if (!isExpectedDeath) {
                coroutineScope.launch {
                    onUnexpectedDeath.send(Unit)
                }
            }

            isExpectedDeath = false
        }
    }

    private var startJob: Job? = null

    fun pingBinder(): Boolean {
        synchronized(systemBridgeLock) {
            return systemBridgeFlow.value?.asBinder()?.pingBinder() == true
        }
    }

    /**
     * This is called by the SystemBridgeBinderProvider content provider.
     */
    fun onBinderReceived(binder: IBinder) {
        val systemBridge = ISystemBridge.Stub.asInterface(binder)

        synchronized(systemBridgeLock) {
            systemBridge.asBinder().linkToDeath(deathRecipient, 0)
            this.systemBridgeFlow.update { systemBridge }

            // Only turn on the ADB options to prevent killing if it is running under
            // the ADB shell user
            if (systemBridge.processUid == Process.SHELL_UID) {
                preventSystemBridgeKilling(systemBridge)
            }
        }
    }

    override fun <T> run(block: (ISystemBridge) -> T): KMResult<T> {
        try {
            val systemBridge = systemBridgeFlow.value ?: return SystemBridgeError.Disconnected

            return Success(block(systemBridge))
        } catch (e: RemoteException) {
            return KMError.Exception(e)
        }
    }

    override fun stopSystemBridge() {
        synchronized(systemBridgeLock) {
            isExpectedDeath = true

            try {
                systemBridgeFlow.value?.destroy()
            } catch (e: RemoteException) {
                // This is expected to throw an exception because the destroy() method kills
                // the process.
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun startWithAdb() {
        if (startJob?.isActive == true) {
            Timber.i("System Bridge is already starting")
            return
        }

        startJob = coroutineScope.launch {
            starter.startWithAdb()
        }
    }

    private fun preventSystemBridgeKilling(systemBridge: ISystemBridge) {
        val deviceId: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ctx.deviceId
            } else {
                -1
            }

        systemBridge.grantPermission(Manifest.permission.WRITE_SECURE_SETTINGS, deviceId)
        Timber.i("Granted WRITE_SECURE_SETTINGS permission with System Bridge")

        if (ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.WRITE_SECURE_SETTINGS
            ) == PERMISSION_GRANTED
        ) {
            // Disable automatic revoking of ADB pairings
            SettingsUtils.putGlobalSetting(
                ctx,
                "adb_allowed_connection_time",
                0
            )

            // Enable USB debugging so the Shell user can keep running in the background
            // even when disconnected from the WiFi network
            SettingsUtils.putGlobalSetting(
                ctx,
                "adb_enabled",
                1
            )
        }
    }

    override fun startWithRoot() {
        if (startJob?.isActive == true) {
            Timber.i("System Bridge is already starting")
            return
        }

        startJob = coroutineScope.launch {
            starter.startWithRoot()
        }
    }

    override fun startWithShizuku() {
        starter.startWithShizuku()
    }
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.Q)
interface SystemBridgeConnectionManager {
    val isConnected: Flow<Boolean>
    val onUnexpectedDeath: Channel<Unit>

    fun <T> run(block: (ISystemBridge) -> T): KMResult<T>
    fun stopSystemBridge()

    fun startWithRoot()
    fun startWithShizuku()
    fun startWithAdb()
}