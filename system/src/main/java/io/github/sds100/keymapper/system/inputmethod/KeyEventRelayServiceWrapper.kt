package io.github.sds100.keymapper.system.inputmethod

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import android.view.KeyEvent
import android.view.MotionEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.api.IKeyEventRelayService
import io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback
import io.github.sds100.keymapper.common.BuildConfigProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyEventRelayServiceWrapperImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val buildConfigProvider: BuildConfigProvider,
) : KeyEventRelayServiceWrapper {

    private val keyEventRelayServiceLock: Any = Any()
    private var keyEventRelayService: IKeyEventRelayService? = null

    private val serviceConnection: ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                synchronized(keyEventRelayServiceLock) {
                    keyEventRelayService = IKeyEventRelayService.Stub.asInterface(service)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                synchronized(keyEventRelayServiceLock) {
                    // Do not unregister the callback in onServiceDisconnected
                    // because the connection is already broken at that point and it
                    // will fail.

                    keyEventRelayService = null
                }
            }
        }

    override fun sendKeyEvent(
        event: KeyEvent,
        targetPackageName: String,
        callbackId: String,
    ): Boolean {
        if (keyEventRelayService == null) {
            return false
        }

        try {
            return keyEventRelayService!!.sendKeyEvent(event, targetPackageName, callbackId)
        } catch (e: DeadObjectException) {
            keyEventRelayService = null
            return false
        }
    }

    override fun sendMotionEvent(
        event: MotionEvent,
        targetPackageName: String,
        callbackId: String,
    ): Boolean {
        if (keyEventRelayService == null) {
            return false
        }

        try {
            return keyEventRelayService!!.sendMotionEvent(event, targetPackageName, callbackId)
        } catch (e: DeadObjectException) {
            keyEventRelayService = null
            return false
        }
    }

    override fun registerClient(id: String, callback: IKeyEventRelayServiceCallback) {
        keyEventRelayService?.registerCallback(callback, id)
    }

    override fun unregisterClient(id: String) {
        keyEventRelayService?.unregisterCallback(id)
    }

    fun bind() {
        try {
            val relayServiceIntent = Intent()
            val component =
                ComponentName(
                    buildConfigProvider.packageName,
                    "io.github.sds100.keymapper.api.KeyEventRelayService",
                )
            relayServiceIntent.setComponent(component)
            val isSuccess =
                ctx.bindService(relayServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

            if (!isSuccess) {
                ctx.unbindService(serviceConnection)
            }
        } catch (e: SecurityException) {
            // Docs say to unbind if there is a security exception.
            ctx.unbindService(serviceConnection)
        }
    }

    fun unbind() {
        try {
            ctx.unbindService(serviceConnection)
        } catch (e: RemoteException) {
            // do nothing
        } catch (e: IllegalArgumentException) {
            // an exception is thrown if you unbind from a service
            // while there is no registered connection.
        }
    }
}

interface KeyEventRelayServiceWrapper {
    fun registerClient(id: String, callback: IKeyEventRelayServiceCallback)
    fun unregisterClient(id: String)
    fun sendKeyEvent(event: KeyEvent, targetPackageName: String, callbackId: String): Boolean
    fun sendMotionEvent(event: MotionEvent, targetPackageName: String, callbackId: String): Boolean
}
