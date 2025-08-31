package io.github.sds100.keymapper.sysbridge.manager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.DeadObjectException
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.os.Process
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.success
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.ktx.TAG
import io.github.sds100.keymapper.sysbridge.starter.SystemBridgeStarter
import io.github.sds100.keymapper.sysbridge.utils.SystemBridgeError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val buildConfigProvider: BuildConfigProvider,
) : SystemBridgeConnectionManager {

    private val systemBridgeLock: Any = Any()
    private var systemBridgeFlow: MutableStateFlow<ISystemBridge?> = MutableStateFlow(null)

    override val connectionState: MutableStateFlow<SystemBridgeConnectionState> =
        MutableStateFlow<SystemBridgeConnectionState>(
            SystemBridgeConnectionState.Disconnected(
                time = SystemClock.elapsedRealtime(), isExpected = true
            )
        )
    private var isExpectedDeath: Boolean = false

    private val deathRecipient: DeathRecipient = DeathRecipient {
        synchronized(systemBridgeLock) {
            Timber.e("System Bridge has died")

            systemBridgeFlow.update { null }

            connectionState.update {
                SystemBridgeConnectionState.Disconnected(
                    time = SystemClock.elapsedRealtime(),
                    isExpected = isExpectedDeath
                )
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
    @SuppressLint("LogNotTimber")
    fun onBinderReceived(binder: IBinder) {
        val systemBridge = ISystemBridge.Stub.asInterface(binder)

        synchronized(systemBridgeLock) {
            if (systemBridge.versionCode == buildConfigProvider.versionCode) {
                // Only link to death if it is the same version code so restarting it
                // doesn't send a death message
                systemBridge.asBinder().linkToDeath(deathRecipient, 0)

                this.systemBridgeFlow.update { systemBridge }

                // Only turn on the ADB options to prevent killing if it is running under
                // the ADB shell user
                if (systemBridge.processUid == Process.SHELL_UID) {
                    preventSystemBridgeKilling(systemBridge)
                }

                connectionState.update {
                    SystemBridgeConnectionState.Connected(
                        time = SystemClock.elapsedRealtime(),
                    )
                }
            } else {
                coroutineScope.launch(Dispatchers.IO) {
                    // Can not use Timber because the content provider is called before the application's
                    // onCreate where the Timber Tree is installed. The content provider then
                    // calls this message.
                    Log.w(
                        TAG,
                        "System Bridge version mismatch! Restarting it. App: ${buildConfigProvider.versionCode}, System Bridge: ${systemBridge.versionCode}"
                    )

                    restartSystemBridge(systemBridge)
                }
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private suspend fun restartSystemBridge(systemBridge: ISystemBridge) {
        starter.startSystemBridge(executeCommand = { command ->
            try {
                systemBridge.executeCommand(command)!!.success()
            } catch (_: DeadObjectException) {
                // This exception is expected since it is killing the system bridge
                Success("")
            } catch (e: Exception) {
                KMError.Exception(e)
            }
        }).onFailure { error ->
            Log.e(TAG, "Failed to restart System Bridge: $error")
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
    val connectionState: StateFlow<SystemBridgeConnectionState>

    fun <T> run(block: (ISystemBridge) -> T): KMResult<T>
    fun stopSystemBridge()

    fun startWithRoot()
    fun startWithShizuku()
    fun startWithAdb()
}