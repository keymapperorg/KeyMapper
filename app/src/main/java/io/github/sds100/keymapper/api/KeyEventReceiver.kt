package io.github.sds100.keymapper.api

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.KeyEvent
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.system.phone.CallState
import io.github.sds100.keymapper.system.phone.PhoneAdapter
import timber.log.Timber

/**
 * Created by sds100 on 30/01/2022.
 */
class KeyEventReceiver : Service() {

    private val phoneAdapter: PhoneAdapter by lazy { ServiceLocator.phoneAdapter(this) }

    private val binderInterface: IKeyEventReceiver = object : IKeyEventReceiver.Stub() {
        override fun onKeyEvent(event: KeyEvent?): Boolean {
            synchronized(callbackLock) {
                Timber.d("KeyEventReceiver: onKeyEvent ${event?.keyCode}")

                if (event == null || callback == null || !shouldForwardKeyEvents()) {
                    Timber.d("KeyEventReceiver: don't forward key event because not in a call")
                    return false
                }

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

    private fun shouldForwardKeyEvents(): Boolean {
        val callState = phoneAdapter.getCallState()
        return callState == CallState.IN_PHONE_CALL || callState == CallState.RINGING
    }
}