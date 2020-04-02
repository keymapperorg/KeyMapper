package io.github.sds100.keymapper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
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

/**
 * Created by sds100 on 31/03/2020.
 */

class KeyMapperImeService : InputMethodService() {
    companion object {
        const val ACTION_INPUT_KEYCODE = "$PACKAGE_NAME.INPUT_KEYCODE"
        const val ACTION_INPUT_KEYEVENT = "$PACKAGE_NAME.INPUT_KEYEVENT"
        const val ACTION_INPUT_TEXT = "$PACKAGE_NAME.INPUT_TEXT"

        const val EXTRA_KEYEVENT = "extra_keyevent"
        const val EXTRA_KEYCODE = "extra_keycode"
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

                    ACTION_INPUT_KEYEVENT -> {
                        intent.getParcelableExtra<KeyEvent>(EXTRA_KEYEVENT)?.let { keyEvent ->
                            currentInputConnection.sendKeyEvent(keyEvent)
                        }
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
        intentFilter.addAction(ACTION_INPUT_KEYEVENT)

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