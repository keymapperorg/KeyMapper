package io.github.sds100.keymapper.sysbridge.manager

import android.annotation.SuppressLint
import android.os.Build
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.os.RemoteException
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.starter.SystemBridgeStarter
import io.github.sds100.keymapper.sysbridge.utils.SystemBridgeError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles starting, stopping and (dis)connecting to the system bridge.
 */
@Singleton
class SystemBridgeConnectionManagerImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val starter: SystemBridgeStarter,
) : SystemBridgeConnectionManager {

    private val systemBridgeLock: Any = Any()
    private var systemBridgeFlow: MutableStateFlow<ISystemBridge?> = MutableStateFlow(null)

    override val isConnected: Flow<Boolean> = systemBridgeFlow.map { it != null }

    private val deathRecipient: DeathRecipient = DeathRecipient {
        synchronized(systemBridgeLock) {
            systemBridgeFlow.update { null }
        }
    }

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
            try {
                systemBridgeFlow.value?.destroy()
            } catch (_: RemoteException) {
                deathRecipient.binderDied()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun startWithAdb() {
        coroutineScope.launch {
            starter.startWithAdb()
        }
    }

    override fun startWithRoot() {
        coroutineScope.launch {
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

    fun <T> run(block: (ISystemBridge) -> T): KMResult<T>
    fun stopSystemBridge()

    fun startWithRoot()
    fun startWithShizuku()
    fun startWithAdb()
}