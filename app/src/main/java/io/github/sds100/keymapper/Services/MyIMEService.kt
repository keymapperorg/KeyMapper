package io.github.sds100.keymapper.Services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService

/**
 * Created by sds100 on 28/09/2018.
 */
class MyIMEService : InputMethodService() {
    companion object {
        const val ACTION_INPUT_KEYCODE = "io.github.sds100.keymapper.INPUT_KEYCODE"
        const val ACTION_INPUT_TEXT = "io.github.sds100.keymapper.INPUT_TEXT"

        const val EXTRA_KEYCODE = "extra_keycode"
        const val EXTRA_TEXT = "extra_text"
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