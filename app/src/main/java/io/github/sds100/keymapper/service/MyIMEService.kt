package io.github.sds100.keymapper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import io.github.sds100.keymapper.Constants

/**
 * Created by sds100 on 28/09/2018.
 */
class MyIMEService : InputMethodService() {
    companion object {
        const val ACTION_INPUT_KEYCODE = "io.github.sds100.keymapper.INPUT_KEYCODE"
        const val ACTION_INPUT_TEXT = "io.github.sds100.keymapper.INPUT_TEXT"

        const val EXTRA_KEYCODE = "extra_keycode"
        const val EXTRA_TEXT = "extra_text"

        fun isServiceEnabled(ctx: Context): Boolean {
            val imeService = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            val enabledMethods = imeService.enabledInputMethodList

            return enabledMethods.any { it.packageName == Constants.PACKAGE_NAME }
        }

        fun getImeId(ctx: Context): String {
            val imeManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            return imeManager.inputMethodList.find { it.packageName == Constants.PACKAGE_NAME }!!.id
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

            return chosenImeId.contains(Constants.PACKAGE_NAME)
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
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_INPUT_KEYCODE)
        intentFilter.addAction(ACTION_INPUT_TEXT)

        registerReceiver(mBroadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
    }
}