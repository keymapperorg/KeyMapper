package io.github.sds100.keymapper.sysbridge.manager

import android.annotation.SuppressLint
import android.os.Build
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles starting, stopping and (dis)connecting to the system bridge.
 */
@Singleton
class SystemBridgeManagerImpl @Inject constructor() : SystemBridgeManager {

    private val systemBridgeLock: Any = Any()
    private var systemBridge: ISystemBridge? = null

    private val connectionsLock: Any = Any()
    private val connections: MutableSet<SystemBridgeConnection> = mutableSetOf()

    private val deathRecipient: DeathRecipient = DeathRecipient {
        synchronized(systemBridgeLock) {
            systemBridge = null
        }

        synchronized(connectionsLock) {
            for (connection in connections) {
                connection.onBindingDied()
            }
        }
    }

    fun pingBinder(): Boolean {
        synchronized(systemBridgeLock) {
            return systemBridge?.asBinder()?.pingBinder() == true
        }
    }

    /**
     * This is called by the SystemBridgeBinderProvider content provider.
     */
    fun onBinderReceived(binder: IBinder) {
        val systemBridge = ISystemBridge.Stub.asInterface(binder)

        synchronized(systemBridgeLock) {
            systemBridge.asBinder().linkToDeath(deathRecipient, 0)
            this.systemBridge = systemBridge
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

    override fun startWithShizuku() {
        TODO("Not yet implemented")
    }

    override fun startWithAdb() {
        TODO("Not yet implemented")
    }

    override fun startWithRoot() {
        TODO("Not yet implemented")
    }

    override fun stopSystemBridge() {
        TODO("Not yet implemented")
    }
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.Q)
interface SystemBridgeManager {
    fun registerConnection(connection: SystemBridgeConnection)
    fun unregisterConnection(connection: SystemBridgeConnection)

    fun startWithShizuku()
    fun startWithAdb()
    fun startWithRoot()
    fun stopSystemBridge()
}