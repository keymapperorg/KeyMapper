package io.github.sds100.keymapper

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Created by sds100 on 16/07/2018.
 */

class MyAccessibilityService : AccessibilityService() {
    companion object {

        const val ACTION_RECORD_TRIGGER = "io.github.sds100.keymapper.RECORD_TRIGGER"
        const val ACTION_STOP_RECORDING_TRIGGER = "io.github.sds100.keymapper.STOP_RECORDING_TRIGGER"
        const val ACTION_CLEAR_PRESSED_KEYS = "io.github.sds100.keymapper.CLEAR_PRESSED_KEYS"
        const val ACTION_GET_PRESSED_KEYS = "io.github.sds100.keymapper.GET_PRESSED_KEYS"

        /**
         * Enable this accessibility service in settings. REQUIRES ROOT
         */
        fun enableServiceInSettings() {
            val className = MyAccessibilityService::class.java.name
            val packageName = this::class.java.`package`.name

            executeRootCommand("settings put secure enabled_accessibility_services $packageName/$className")
        }

        /**
         * Disable this accessibility service in settings. REQUIRES ROOT
         */
        fun disableServiceInSettings() {
            val packageName = this::class.java.`package`.name

            executeRootCommand("settings put secure enabled_accessibility_services $packageName")
        }

        private fun executeRootCommand(command: String) {
            Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        }
    }

    private val mNewKeyMapActivityBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                ACTION_RECORD_TRIGGER -> {
                    mRecordingTrigger = true
                }

                ACTION_STOP_RECORDING_TRIGGER -> {
                    mRecordingTrigger = false

                    mPressedKeys.clear()
                }

                ACTION_CLEAR_PRESSED_KEYS -> {
                    mPressedKeys.clear()
                }
            }
        }
    }

    /**
     * The keys currently being held down.
     */
    private val mPressedKeys = mutableListOf<Int>()

    private var mRecordingTrigger = false

    override fun onServiceConnected() {
        super.onServiceConnected()

        //listen for events from NewKeyMapActivity
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RECORD_TRIGGER)
        intentFilter.addAction(ACTION_STOP_RECORDING_TRIGGER)
        intentFilter.addAction(ACTION_CLEAR_PRESSED_KEYS)

        registerReceiver(mNewKeyMapActivityBroadcastReceiver, intentFilter)
    }

    override fun onInterrupt() {

    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mNewKeyMapActivityBroadcastReceiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (mRecordingTrigger) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                //only add the key to the trigger if it isn't already a part of the trigger
                if (!mPressedKeys.contains(event.keyCode)) {
                    //tell NewKeyMapActivity to add the chip
                    val intent = Intent(NewKeyMapActivity.ACTION_ADD_KEY_CHIP)
                    intent.putExtra(NewKeyMapActivity.EXTRA_KEY_EVENT, event)

                    sendBroadcast(intent)

                    mPressedKeys.add(event.keyCode)
                }
            }

            //Don't allow the key to do anything when recording a trigger
            return true
        }

        return super.onKeyEvent(event)
    }
}