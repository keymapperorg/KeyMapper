package io.github.sds100.keymapper.Services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.Activities.NewKeymapActivity
import io.github.sds100.keymapper.Data.KeyMapRepository
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.SystemAction
import io.github.sds100.keymapper.Utils.RootUtils

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

            RootUtils.executeRootCommand(
                    "settings put secure enabled_accessibility_services io.github.sds100.keymapper/$className")
        }

        /**
         * Disable this accessibility service. REQUIRES ROOT
         */
        fun disableServiceInSettings() {
            RootUtils.executeRootCommand("settings put secure enabled_accessibility_services \"\"")
        }
    }

    /**
     * Broadcast receiver for all intents sent from within the app.
     */
    private val mBroadcastReceiver = object : BroadcastReceiver() {
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

        //listen for events from NewKeymapActivity
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RECORD_TRIGGER)
        intentFilter.addAction(ACTION_STOP_RECORDING_TRIGGER)
        intentFilter.addAction(ACTION_CLEAR_PRESSED_KEYS)
        intentFilter.addAction(ACTION_UPDATE_KEYMAP_CACHE)

        registerReceiver(mBroadcastReceiver, intentFilter)

        //when the accessibility service starts
        getKeyMapListFromRepository()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event != null) {
            if (mRecordingTrigger) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    //only add the key to the trigger if it isn't already a part of the trigger
                    if (!mPressedKeys.contains(event.keyCode)) {
                        //tell NewKeymapActivity to add the chip
                        val intent = Intent(NewKeymapActivity.ACTION_ADD_KEY_CHIP)
                        intent.putExtra(NewKeymapActivity.EXTRA_KEY_EVENT, event)

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

                    //find the keymap associated with the trigger being pressed
                    val keyMap = mKeyMapListCache.find { keyMap ->
                        keyMap.triggerList.any { trigger -> trigger.keys == mPressedTriggerKeys }
                    }

                    if (keyMap != null) {
                        performAction(keyMap.action)
                    }

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

    private fun performAction(action: Action) {
        when (action.type) {
            ActionType.APP -> {
                val intent = packageManager.getLaunchIntentForPackage(action.data)
                startActivity(intent)
            }

            ActionType.APP_SHORTCUT -> {
                val intent = Intent.parseUri(action.data, 0)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            ActionType.TEXT_BLOCK -> {
                val intent = Intent(MyIMEService.ACTION_INPUT_TEXT)
                //put the text in the intent
                intent.putExtra(MyIMEService.EXTRA_TEXT, action.data)

                sendBroadcast(intent)
            }

            ActionType.SYSTEM_ACTION -> performSystemAction(SystemAction.valueOf(action.data))

            else -> {
                if (action.type == ActionType.KEYCODE || action.type == ActionType.KEY) {
                    val intent = Intent(MyIMEService.ACTION_INPUT_KEYCODE)
                    //put the keycode in the intent
                    intent.putExtra(MyIMEService.EXTRA_KEYCODE, action.data.toInt())

                    sendBroadcast(intent)
                }
            }
        }
    }

    private fun performSystemAction(action: SystemAction) {
        when (action) {
            SystemAction.ACTION_ENABLE_WIFI -> {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE)
                        as WifiManager
                wifiManager.isWifiEnabled = true
            }

            SystemAction.ACTION_DISABLE_WIFI -> {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE)
                        as WifiManager
                wifiManager.isWifiEnabled = false
            }

            SystemAction.ACTION_TOGGLE_WIFI -> {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE)
                        as WifiManager
                //toggle wifi
                wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
            }
        }
    }

    private val KeyEvent.isVolumeKey: Boolean
        get() = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
}