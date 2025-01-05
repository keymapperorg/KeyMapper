package io.github.sds100.keymapper.system.inputmethod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback
import io.github.sds100.keymapper.api.KeyEventRelayServiceWrapperImpl

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

    private val keyEventReceiverCallback: IKeyEventRelayServiceCallback =
        object : IKeyEventRelayServiceCallback.Stub() {
            override fun onKeyEvent(event: KeyEvent?, sourcePackageName: String?): Boolean {
                // Only accept key events from Key Mapper
                if (sourcePackageName != Constants.PACKAGE_NAME) {
                    return false
                }

                return currentInputConnection?.sendKeyEvent(event) ?: false
            }

            override fun onMotionEvent(event: MotionEvent?, sourcePackageName: String?): Boolean {
                // Do nothing if the IME receives a motion event.
                return false
            }
        }

    private val keyEventRelayServiceWrapper: KeyEventRelayServiceWrapperImpl by lazy {
        KeyEventRelayServiceWrapperImpl(this, keyEventReceiverCallback)
    }

    override fun onCreate() {
        super.onCreate()

        val filter = IntentFilter()
        filter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN)
        filter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP)
        filter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP)
        filter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_TEXT)

        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        keyEventRelayServiceWrapper.onCreate()
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        val consume = keyEventRelayServiceWrapper.sendMotionEvent(event, Constants.PACKAGE_NAME)

        return if (consume) {
            true
        } else {
            super.onGenericMotionEvent(event)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val consume = keyEventRelayServiceWrapper.sendKeyEvent(event, Constants.PACKAGE_NAME)

        return if (consume) {
            true
        } else {
            super.onKeyUp(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val consume = keyEventRelayServiceWrapper.sendKeyEvent(event, Constants.PACKAGE_NAME)

        return if (consume) {
            true
        } else {
            super.onKeyUp(keyCode, event)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        keyEventRelayServiceWrapper.onDestroy()

        super.onDestroy()
    }
}
