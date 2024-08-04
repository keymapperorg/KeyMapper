package io.github.sds100.keymapper.api

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.IBinder
import android.view.KeyEvent

/**
 * This handles connecting to the relay service and exposes an interface
 * so other parts of the app can get a reference to the service even when it isn't
 * bound yet. This class is copied to the Key Mapper GUI Keyboard app as well.
 */
class KeyEventRelayServiceWrapperImpl(
    context: Context,
    private val callback: IKeyEventRelayServiceCallback,
) : KeyEventRelayServiceWrapper {
    private val ctx: Context = context.applicationContext

    private val keyEventRelayServiceLock: Any = Any()
    private var keyEventRelayService: IKeyEventRelayService? = null

    private val keyEventReceiverConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            synchronized(keyEventRelayServiceLock) {
                keyEventRelayService = IKeyEventRelayService.Stub.asInterface(service)
                keyEventRelayService?.registerCallback(callback)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(keyEventRelayServiceLock) {
                keyEventRelayService?.unregisterCallback()
                keyEventRelayService = null
            }
        }
    }

    override fun sendKeyEvent(event: KeyEvent?, targetPackageName: String?): Boolean {
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

    fun bind() {
        Intent(ctx, KeyEventRelayService::class.java).also { intent ->
            ctx.bindService(intent, keyEventReceiverConnection, Service.BIND_AUTO_CREATE)
        }
    }

    fun unbind() {
        try {
            ctx.unbindService(keyEventReceiverConnection)
        } catch (e: DeadObjectException) {
            // do nothing
        }
    }
}

interface KeyEventRelayServiceWrapper {
    fun sendKeyEvent(event: KeyEvent?, targetPackageName: String?): Boolean
}
