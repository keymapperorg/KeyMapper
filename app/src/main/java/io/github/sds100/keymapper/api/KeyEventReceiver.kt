package io.github.sds100.keymapper.api

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.KeyEvent
import timber.log.Timber

/**
 * Created by sds100 on 30/01/2022.
 */
class KeyEventReceiver : Service() {

    private val binderInterface: IKeyEventReceiver = object : IKeyEventReceiver.Stub() {
        override fun onKeyEvent(event: KeyEvent?): Boolean {
            synchronized(callbackLock) {
                Timber.d("KeyEventReceiver: onKeyEvent ${event?.keyCode}")

                return callback!!.onKeyEvent(event)
            }
        }

        override fun registerCallback(client: IKeyEventReceiverCallback?) {
            synchronized(callbackLock) {
                callback = client
            }
        }

        override fun unregisterCallback(client: IKeyEventReceiverCallback?) {
            synchronized(callbackLock) {
                callback = null
            }
        }
    }

    private val callbackLock: Any = Any()
    private var callback: IKeyEventReceiverCallback? = null

    override fun onBind(intent: Intent?): IBinder? {
        return binderInterface.asBinder()
    }
}