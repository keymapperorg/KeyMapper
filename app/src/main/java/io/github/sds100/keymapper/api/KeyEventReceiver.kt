package io.github.sds100.keymapper.api

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.KeyEvent
import timber.log.Timber

/**
 * This service is used as a relay between the accessibility service and input method service to pass
 * key events back and forth. A separate service has to be used because you can't bind to an
 * accessibility service. The input method service sends key events to this service by calling
 * onKeyEvent(), and the accessibility service registers with the callback to receive the
 * key events being sent.
 *
 * This was implemented in issue #850 for the action to answer phone calls because Android doesn't
 * pass volume down key events to the accessibility service when the phone is ringing or it is
 * in a phone call.
 */
class KeyEventReceiver : Service() {

    private val binderInterface: IKeyEventReceiver = object : IKeyEventReceiver.Stub() {
        override fun onKeyEvent(event: KeyEvent?): Boolean {
            synchronized(callbackLock) {
                Timber.d("KeyEventReceiver: onKeyEvent ${event?.keyCode}")

                if (callback != null) {
                    return callback!!.onKeyEvent(event)
                } else {
                    return false
                }
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

    override fun onBind(intent: Intent?): IBinder? = binderInterface.asBinder()
}
