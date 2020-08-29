package io.github.sds100.keymapper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.util.result.KeyMapperImeNotFound
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import splitties.init.appCtx
import splitties.systemservices.inputMethodManager

/**
 * Created by sds100 on 31/03/2020.
 */

class KeyMapperImeService : InputMethodService() {
    companion object {
        //DON'T CHANGE THESE!!!
        private const val KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP = "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_DOWN_UP"
        private const val KEY_MAPPER_INPUT_METHOD_ACTION_TEXT = "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_TEXT"

        private const val KEY_MAPPER_INPUT_METHOD_EXTRA_KEYCODE = "io.github.sds100.keymapper.inputmethod.EXTRA_KEYCODE"
        private const val KEY_MAPPER_INPUT_METHOD_EXTRA_METASTATE = "io.github.sds100.keymapper.inputmethod.EXTRA_METASTATE"
        private const val KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT = "io.github.sds100.keymapper.inputmethod.EXTRA_TEXT"

        fun isServiceEnabled(): Boolean {
            val enabledMethods = inputMethodManager.enabledInputMethodList ?: return false

            return enabledMethods.any { it.packageName == PACKAGE_NAME }
        }

        /**
         * Get the id for the Key Mapper input input_method.
         */
        fun getImeId(): Result<String> {

            val inputMethod = inputMethodManager.inputMethodList.find { it.packageName == PACKAGE_NAME }
                ?: return KeyMapperImeNotFound()

            return Success(inputMethod.id)
        }

        /**
         * @return whether the Key Mapper input input_method is chosen
         */
        fun isInputMethodChosen(): Boolean {
            //get the current input input_method
            val chosenImeId = Settings.Secure.getString(
                appCtx.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )

            return inputMethodManager.inputMethodList.find { it.id == chosenImeId }?.packageName == PACKAGE_NAME
        }
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return

            fun getKeyCode() = intent.getIntExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_KEYCODE, -1)
            fun getMetaState() = intent.getIntExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_METASTATE, 0)

            when (action) {
                KEY_MAPPER_INPUT_METHOD_ACTION_TEXT -> {
                    val text = intent.getStringExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT) ?: return

                    currentInputConnection?.commitText(text, 1)
                }

                KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP -> {
                    val keyCode = getKeyCode()
                    if (keyCode == -1) return

                    val eventTime = SystemClock.uptimeMillis()

                    val downEvent = KeyEvent(eventTime, eventTime,
                        KeyEvent.ACTION_DOWN, keyCode, 0, getMetaState())

                    currentInputConnection?.sendKeyEvent(downEvent)

                    val upEvent = KeyEvent(eventTime, SystemClock.uptimeMillis(),
                        KeyEvent.ACTION_UP, keyCode, 0)

                    currentInputConnection?.sendKeyEvent(upEvent)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        IntentFilter().apply {
            addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP)
            addAction(KEY_MAPPER_INPUT_METHOD_ACTION_TEXT)

            registerReceiver(mBroadcastReceiver, this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
    }
}