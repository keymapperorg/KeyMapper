package io.github.sds100.keymapper.Services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.Activities.NewKeyMapActivity
import io.github.sds100.keymapper.Data.KeyMapRepository
import io.github.sds100.keymapper.KeyMap

/**
 * Created by sds100 on 16/07/2018.
 */

class MyAccessibilityService : AccessibilityService() {
    companion object {
        const val EXTRA_KEYMAP_CACHE_JSON = "extra_keymap_cache_json"

        const val ACTION_RECORD_TRIGGER = "io.github.sds100.keymapper.RECORD_TRIGGER"
        const val ACTION_STOP_RECORDING_TRIGGER = "io.github.sds100.keymapper.STOP_RECORDING_TRIGGER"
        const val ACTION_CLEAR_PRESSED_KEYS = "io.github.sds100.keymapper.CLEAR_PRESSED_KEYS"
        const val ACTION_UPDATE_KEYMAP_CACHE = "io.github.sds100.keymapper.UPDATE_KEYMAP_CACHE"

        /**
         * Enable this accessibility service. REQUIRES ROOT
         */
        fun enableServiceInSettings() {
            val className = MyAccessibilityService::class.java.name

            executeRootCommand("settings put secure enabled_accessibility_services io.github.sds100.keymapper/$className")
        }

        /**
         * Disable this accessibility service. REQUIRES ROOT
         */
        fun disableServiceInSettings() {
            executeRootCommand("settings put secure enabled_accessibility_services \"\"")
        }

        private fun executeRootCommand(command: String) {
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Broadcast receiver for all intents sent from within the app.
     */
    private val mAppBroadcastReceiver = object : BroadcastReceiver() {
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

                ACTION_UPDATE_KEYMAP_CACHE -> {
                    //deserialize the keymap list
                    val jsonString = intent.getStringExtra(EXTRA_KEYMAP_CACHE_JSON)

                    if (jsonString != null) {
                        mKeyMapListCache = Gson().fromJson(jsonString)
                    }
                }
            }
        }
    }

    /**
     * A cached copy of the keymaps in the database
     */
    private var mKeyMapListCache: List<KeyMap> = listOf()

    /**
     * The keys currently being held down.
     */
    private val mPressedKeys = mutableListOf<Int>()

    /**
     * When all the keys that map to a specific trigger are pressed, they are put in here.
     * E.g when Ctrl + J is pressed, the contents of this list will be the keycodes for Ctrl + J
     */
    private var mPressedTriggerKeys = mutableListOf<Int>()

    private var mRecordingTrigger = false

    override fun onServiceConnected() {
        super.onServiceConnected()

        //listen for events from NewKeyMapActivity
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RECORD_TRIGGER)
        intentFilter.addAction(ACTION_STOP_RECORDING_TRIGGER)
        intentFilter.addAction(ACTION_CLEAR_PRESSED_KEYS)
        intentFilter.addAction(ACTION_UPDATE_KEYMAP_CACHE)

        registerReceiver(mAppBroadcastReceiver, intentFilter)

        //when the accessibility service starts
        getKeyMapListFromRepository()
    }

    override fun onInterrupt() {

    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mAppBroadcastReceiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event != null) {
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

            } else {

                //when a key is pressed down
                if (event.action == KeyEvent.ACTION_DOWN) {
                    mPressedKeys.add(event.keyCode)

                    //when a key is lifted
                } else if (event.action == KeyEvent.ACTION_UP) {
                    mPressedKeys.remove(event.keyCode)

                    if (mPressedTriggerKeys.isNotEmpty()) {
                        if (mPressedTriggerKeys.contains(event.keyCode)) {
                            mPressedTriggerKeys.remove(event.keyCode)

                            /* pass the volume key event to the system otherwise strange behaviour
                            * happens where the volume key is constantly being pressed */

                            if (event.isVolumeKey) {
                                return super.onKeyEvent(event)
                            }

                            return true
                        }
                    }
                }

                if (isTrigger(mPressedKeys)) {
                    mPressedTriggerKeys = mPressedKeys.toMutableList()

                    return true
                }
            }
        }

        return super.onKeyEvent(event)
    }

    private fun getKeyMapListFromRepository() {
        val list = KeyMapRepository.getInstance(baseContext).keyMapList.value

        if (list != null) {
            mKeyMapListCache = list
        }
    }

    /**
     * @param keys the combination of keycodes being pressed to check.
     * @return whether a key combination is registered as a trigger in the keymap cache
     */
    private fun isTrigger(keys: MutableList<Int>): Boolean {
        return mKeyMapListCache.any { keyMap ->

            /* do any of the trigger lists for each keymap contain a trigger which matches the
             * keys being pressed?*/
            keyMap.triggerList.any { trigger ->
                trigger.keys.toTypedArray().contentEquals(keys.toTypedArray())
            }
        }
    }

    private val KeyEvent.isVolumeKey: Boolean
        get() = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
}