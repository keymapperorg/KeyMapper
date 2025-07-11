package io.github.sds100.keymapper.sysbridge.manager

import android.content.Context
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles starting, stopping, (dis)connecting to the system bridge
 */
@Singleton
class SystemBridgeManagerImpl @Inject constructor(
    @ApplicationContext private val ctx: Context
) : SystemBridgeManager {

    fun pingBinder(): Boolean {
        return false
    }

    fun onBinderReceived(binder: IBinder) {

    }
}

interface SystemBridgeManager