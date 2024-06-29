package io.github.sds100.keymapper.system.inputmethod

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.DeadObjectException
import android.os.IBinder
import android.view.KeyEvent
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.api.IKeyEventRelayService
import io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback
import io.github.sds100.keymapper.api.KeyEventRelayService

/**
 * Created by sds100 on 31/03/2020.
 */

class KeyMapperImeService : InputMethodService() {
    companion object {

        // DON'T CHANGE THESE!!!
        private const val KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP =
            "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_DOWN_UP"
        private const val KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN =
            "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_DOWN"
        private const val KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP =
            "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_UP"
        private const val KEY_MAPPER_INPUT_METHOD_ACTION_TEXT =
            "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_TEXT"

        private const val KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT =
            "io.github.sds100.keymapper.inputmethod.EXTRA_TEXT"
        const val KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT =
            "io.github.sds100.keymapper.inputmethod.EXTRA_KEY_EVENT"
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return

            when (action) {
                KEY_MAPPER_INPUT_METHOD_ACTION_TEXT -> {
                    val text = intent.getStringExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT) ?: return

                    currentInputConnection?.commitText(text, 1)
                }

                KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP -> {
                    val downEvent = intent.getParcelableExtra<KeyEvent>(
                        KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT,
                    )
                    currentInputConnection?.sendKeyEvent(downEvent)

                    val upEvent = KeyEvent.changeAction(downEvent, KeyEvent.ACTION_UP)
                    currentInputConnection?.sendKeyEvent(upEvent)
                }

                KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN -> {
                    var downEvent = intent.getParcelableExtra<KeyEvent>(
                        KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT,
                    )

                    downEvent = KeyEvent.changeAction(downEvent, KeyEvent.ACTION_DOWN)

                    currentInputConnection?.sendKeyEvent(downEvent)
                }

                KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP -> {
                    var upEvent = intent.getParcelableExtra<KeyEvent>(
                        KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT,
                    )

                    upEvent = KeyEvent.changeAction(upEvent, KeyEvent.ACTION_UP)

                    currentInputConnection?.sendKeyEvent(upEvent)
                }
            }
        }
    }

    private val keyEventReceiverLock: Any = Any()
    private var keyEventReceiverBinder: IKeyEventRelayService? = null

    private val keyEventReceiverCallback: IKeyEventRelayServiceCallback =
        object : IKeyEventRelayServiceCallback.Stub() {
            override fun onKeyEvent(event: KeyEvent?, sourcePackageName: String?): Boolean {
                // Only accept key events from Key Mapper
                if (sourcePackageName != Constants.PACKAGE_NAME) {
                    return false
                }

                return currentInputConnection?.sendKeyEvent(event) ?: false
            }
        }

    private val keyEventReceiverConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            synchronized(keyEventReceiverLock) {
                keyEventReceiverBinder = IKeyEventRelayService.Stub.asInterface(service)
                keyEventReceiverBinder?.registerCallback(keyEventReceiverCallback)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(keyEventReceiverLock) {
                keyEventReceiverBinder?.unregisterCallback()
                keyEventReceiverBinder = null
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        IntentFilter().apply {
            addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN)
            addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP)
            addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP)
            addAction(KEY_MAPPER_INPUT_METHOD_ACTION_TEXT)

            // TODO use ContextCompat
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(broadcastReceiver, this, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(broadcastReceiver, this)
            }
        }

        Intent(this, KeyEventRelayService::class.java).also { intent ->
            bindService(intent, keyEventReceiverConnection, Service.BIND_AUTO_CREATE)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyEventReceiverBinder == null) {
            return super.onKeyDown(keyCode, event)
        }

        try {
            return keyEventReceiverBinder!!.sendKeyEvent(event, Constants.PACKAGE_NAME)
        } catch (e: DeadObjectException) {
            return super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyEventReceiverBinder == null) {
            return super.onKeyUp(keyCode, event)
        }

        try {
            return keyEventReceiverBinder!!.sendKeyEvent(event, Constants.PACKAGE_NAME)
        } catch (e: DeadObjectException) {
            return super.onKeyUp(keyCode, event)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        unbindService(keyEventReceiverConnection)

        super.onDestroy()
    }
}
