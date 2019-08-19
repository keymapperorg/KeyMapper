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
import android.view.inputmethod.InputMethodManager
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.Result
import io.github.sds100.keymapper.handle
import io.github.sds100.keymapper.result

/**
 * Created by sds100 on 28/09/2018.
 */
class MyIMEService : InputMethodService() {
    companion object {
        const val ACTION_INPUT_KEYCODE = "$PACKAGE_NAME.INPUT_KEYCODE"
        const val ACTION_INPUT_KEYEVENT = "$PACKAGE_NAME.INPUT_KEYEVENT"
        const val ACTION_INPUT_TEXT = "$PACKAGE_NAME.INPUT_TEXT"

        const val EXTRA_KEYEVENT = "extra_keyevent"
        const val EXTRA_KEYCODE = "extra_keycode"
        const val EXTRA_TEXT = "extra_text"

        fun isServiceEnabled(ctx: Context): Boolean {
            val imeService = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            val enabledMethods = imeService.enabledInputMethodList

            return enabledMethods.any { it.packageName == PACKAGE_NAME }
        }

        /**
         * Get the id for the Key Mapper input method.
         */
        fun getImeId(ctx: Context): String? {
            val imeManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            return imeManager.inputMethodList.find { it.packageName == PACKAGE_NAME }.result().handle(
                    onSuccess = { it.id },
                    onFailure = { null }
            )
        }

        /**
         * @return whether the Key Mapper input method is chosen
         */
        fun isInputMethodChosen(ctx: Context): Boolean {
            //get the current input method
            val chosenImeId = Settings.Secure.getString(
                    ctx.contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD
            )

            val imeManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            return imeManager.inputMethodList.find { it.id == chosenImeId }?.packageName == PACKAGE_NAME
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

    private val InputConnection.charCount: Result<Int>
        get() {
            val request = ExtractedTextRequest().apply {
                token = 0
            }

            return getExtractedText(request, 0).text.length.result()
        }
}