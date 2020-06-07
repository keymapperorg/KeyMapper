package io.github.sds100.keymapper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.util.result.KeyMapperImeNotFound
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import splitties.init.appCtx
import splitties.systemservices.inputMethodManager
import timber.log.Timber

/**
 * Created by sds100 on 31/03/2020.
 */

class KeyMapperImeService : InputMethodService() {
    companion object {
        const val ACTION_INPUT_KEYCODE = "$PACKAGE_NAME.INPUT_KEYCODE"
        const val ACTION_INPUT_DOWN_UP = "$PACKAGE_NAME.INPUT_DOWN_UP"
        const val ACTION_INPUT_TEXT = "$PACKAGE_NAME.INPUT_TEXT"

        const val EXTRA_KEYCODE = "extra_keycode"
        const val EXTRA_META_STATE = "extra_meta_state"
        const val EXTRA_TEXT = "extra_text"

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
            if (intent != null) {
                when (intent.action!!) {
                    ACTION_INPUT_KEYCODE -> {
                        val keyCode = intent.getIntExtra(EXTRA_KEYCODE, 0)

                        sendDownUpKeyEvents(keyCode)
                    }

                    ACTION_INPUT_TEXT -> {
                        val text = intent.getStringExtra(EXTRA_TEXT)

                        currentInputConnection.commitText(text, 1)
                    }

                    ACTION_INPUT_DOWN_UP -> {
                        val keyCode = intent.getIntExtra(EXTRA_KEYCODE, -1)
                        val metaState = intent.getIntExtra(EXTRA_META_STATE, 0)

                        if (keyCode == -1) return

                        val eventTime = SystemClock.uptimeMillis()

                        val downEvent = KeyEvent(eventTime, eventTime,
                            KeyEvent.ACTION_DOWN, keyCode, 0, metaState)

                        currentInputConnection.sendKeyEvent(downEvent)

                        Timber.d("input $downEvent")

                        val upEvent = KeyEvent(eventTime, SystemClock.uptimeMillis(),
                            KeyEvent.ACTION_UP, keyCode, 0)

                        currentInputConnection.sendKeyEvent(upEvent)

                        Timber.d("input $upEvent")
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_INPUT_KEYCODE)
        intentFilter.addAction(ACTION_INPUT_TEXT)
        intentFilter.addAction(ACTION_INPUT_DOWN_UP)

        registerReceiver(mBroadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
    }

    private val InputConnection.charCount: Int
        get() {
            val request = ExtractedTextRequest().apply {
                token = 0
            }

            return getExtractedText(request, 0).text.length
        }
}