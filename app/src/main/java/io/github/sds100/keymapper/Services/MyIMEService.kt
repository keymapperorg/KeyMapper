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
        const val EXTRA_KEYCODE = "extra_keycode"
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                when (intent.action!!) {
                    ACTION_INPUT_KEYCODE -> {
                        val keyCode = intent.getIntExtra(EXTRA_KEYCODE, 0)

                        sendDownUpKeyEvents(keyCode)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_INPUT_KEYCODE)

        registerReceiver(mBroadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
    }
}