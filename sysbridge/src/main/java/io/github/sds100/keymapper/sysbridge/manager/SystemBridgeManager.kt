package io.github.sds100.keymapper.sysbridge.manager

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.os.RemoteException
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.sysbridge.ISystemBridge
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
class SystemBridgeManagerImpl @Inject constructor(
    @ApplicationContext private val ctx: Context
) : SystemBridgeManager {

    private val systemBridgeLock: Any = Any()
    private var systemBridge: MutableStateFlow<ISystemBridge?> = MutableStateFlow(null)

    override val isConnected: Flow<Boolean> = systemBridge.map { it != null }

    private val connectionsLock: Any = Any()
    private val connections: MutableSet<SystemBridgeConnection> = mutableSetOf()

    private val deathRecipient: DeathRecipient = DeathRecipient {
        synchronized(systemBridgeLock) {
            systemBridge.update { null }
        }

        synchronized(connectionsLock) {
            for (connection in connections) {
                connection.onBindingDied()
            }
        }
    }

    fun pingBinder(): Boolean {
        synchronized(systemBridgeLock) {
            return systemBridge.value?.asBinder()?.pingBinder() == true
        }
    }

    /**
     * This is called by the SystemBridgeBinderProvider content provider.
     */
    fun onBinderReceived(binder: IBinder) {
        val systemBridge = ISystemBridge.Stub.asInterface(binder)

        synchronized(systemBridgeLock) {
            systemBridge.asBinder().linkToDeath(deathRecipient, 0)
            this.systemBridge.update { systemBridge }
        }

        synchronized(connectionsLock) {
            for (connection in connections) {
                connection.onServiceConnected(systemBridge)
            }
        }
    }

    override fun registerConnection(connection: SystemBridgeConnection) {
        synchronized(connectionsLock) {
            connections.add(connection)
        }
    }

    override fun unregisterConnection(connection: SystemBridgeConnection) {
        synchronized(connectionsLock) {
            connections.remove(connection)
        }
    }

    override fun stopSystemBridge() {
        synchronized(systemBridgeLock) {
            try {
                systemBridge.value?.destroy()
            } catch (_: RemoteException) {
                deathRecipient.binderDied()
            }
        }
    }

}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.Q)
interface SystemBridgeManager {
    val isConnected: Flow<Boolean>

    fun registerConnection(connection: SystemBridgeConnection)
    fun unregisterConnection(connection: SystemBridgeConnection)

    fun stopSystemBridge()
}