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
import io.github.sds100.keymapper.sysbridge.utils.SystemBridgeError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles starting, stopping and (dis)connecting to the system bridge.
 */
@Singleton
class SystemBridgeConnectionManagerImpl @Inject constructor() : SystemBridgeConnectionManager {

    // TODO if auto start is turned on, subscribe to Shizuku Binder listener and when bound, start the service. But only do this once per app process session. If the user stops the service it should remain stopped until key mapper is killed,

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

}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.Q)
interface SystemBridgeConnectionManager {
    val isConnected: Flow<Boolean>

    fun <T> run(block: (ISystemBridge) -> T): KMResult<T>
    fun stopSystemBridge()
}