package io.github.sds100.keymapper.api

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import android.view.KeyEvent

/**
 * This handles connecting to the relay service and exposes an interface
 * so other parts of the app can get a reference to the service even when it isn't
 * bound yet. This class is copied to the Key Mapper GUI Keyboard app as well.
 */
class KeyEventRelayServiceWrapperImpl(
    private val ctx: Context,
    private val callback: IKeyEventRelayServiceCallback,
) : KeyEventRelayServiceWrapper {

    private val keyEventRelayServiceLock: Any = Any()
    private var keyEventRelayService: IKeyEventRelayService? = null

    private val serviceConnection: ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                synchronized(keyEventRelayServiceLock) {
                    keyEventRelayService = IKeyEventRelayService.Stub.asInterface(service)
                    keyEventRelayService?.registerCallback(callback)
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

    fun onCreate() {
        bind()
    }

    fun onDestroy() {
        unbind()
    }

    override fun sendKeyEvent(
        event: KeyEvent?,
        targetPackageName: String?,
    ): Boolean {
        synchronized(keyEventRelayServiceLock) {
            if (keyEventRelayService == null) {
                return false
            }

            try {
                return keyEventRelayService!!.sendKeyEvent(event, targetPackageName)
            } catch (e: DeadObjectException) {
                keyEventRelayService = null
                return false
            }
        }
    }

    private fun bind() {
        try {
            val relayServiceIntent = Intent(ctx, KeyEventRelayService::class.java)
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

    private fun unbind() {
        // Unregister the callback if this input method is unbinding
        // from the relay service. This should not happen in onServiceDisconnected
        // because the connection is already broken at that point and it
        // will fail.
        try {
            keyEventRelayService?.unregisterCallback()
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
    fun sendKeyEvent(event: KeyEvent?, targetPackageName: String?): Boolean
}
