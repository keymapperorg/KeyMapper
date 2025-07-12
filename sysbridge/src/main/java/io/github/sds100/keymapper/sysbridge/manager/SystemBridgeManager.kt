package io.github.sds100.keymapper.sysbridge.manager

import android.content.Context
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles starting, stopping, (dis)connecting to the system bridge
 */
@Singleton
class SystemBridgeManagerImpl @Inject constructor(
    @ApplicationContext private val ctx: Context
) : SystemBridgeManager {

    private val lock: Any = Any()
    private var systemBridge: ISystemBridge? = null

    fun pingBinder(): Boolean {
        return false
    }

    fun onBinderReceived(binder: IBinder) {
        synchronized(lock) {
            this.systemBridge = ISystemBridge.Stub.asInterface(binder)

            this.systemBridge?.sendEvent()
        }
    }
}

interface SystemBridgeManager