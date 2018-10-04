package io.github.sds100.keymapper.Services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.github.salomonbrys.kotson.fromJson
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.Activities.ConfigKeymapActivity
import io.github.sds100.keymapper.Data.KeyMapRepository
import io.github.sds100.keymapper.Utils.RootUtils

/**
 * Created by sds100 on 16/07/2018.
 */

class MyAccessibilityService : AccessibilityService() {
    companion object {
        const val EXTRA_KEYMAP_CACHE_JSON = "extra_keymap_cache_json"
        const val EXTRA_ACTION = "action"

        const val ACTION_RECORD_TRIGGER = "io.github.sds100.keymapper.RECORD_TRIGGER"
        const val ACTION_STOP_RECORDING_TRIGGER = "io.github.sds100.keymapper.STOP_RECORDING_TRIGGER"
        const val ACTION_CLEAR_PRESSED_KEYS = "io.github.sds100.keymapper.CLEAR_PRESSED_KEYS"
        const val ACTION_UPDATE_KEYMAP_CACHE = "io.github.sds100.keymapper.UPDATE_KEYMAP_CACHE"
        const val ACTION_TEST_ACTION = "io.github.sds100.keymapper.TEST_ACTION"
        const val ACTION_RECORD_TRIGGER_TIMER_STOPPED =
                "io.github.sds100.keymapper.RECORD_TRIGGER_TIMER_STOPPED"

        /**
         * How long should the accessibility service record a trigger. In milliseconds.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5000L

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

        /**
         * If the accessibility service is disabled, show a snackbar with a button
         * to enable it in settings
         *
         * @return whether the accessibility service is enabled
         */
        fun isAccessibilityServiceEnabled(ctx: Context, view: View): Boolean {
            /* get a list of all the enabled accessibility services.
         * The AccessibilityManager.getEnabledAccessibilityServices() method just returns an empty
         * list. :(*/
            val settingValue = Settings.Secure.getString(
                    ctx.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

            val isEnabled = settingValue.contains(ctx.packageName)

            //show a snackbar if disabled
            if (!isEnabled) {
                val snackBar = Snackbar.make(
                        view,
                        R.string.error_accessibility_service_disabled,
                        Snackbar.LENGTH_INDEFINITE
                )

                snackBar.setAction(R.string.enable) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    ctx.startActivity(intent)
                }

                snackBar.show()
            }

            return isEnabled
        }
    }

    private val mStopRecordingHandler = Handler()

    /**
     * Broadcast receiver for all intents sent from within the app.
     */
    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                ACTION_RECORD_TRIGGER -> {
                    mRecordingTrigger = true

                    //stop recording a trigger after a set amount of time.
                    mStopRecordingHandler.postDelayed({
                        mRecordingTrigger = false
                        mPressedKeys.clear()

                        sendBroadcast(Intent(ACTION_RECORD_TRIGGER_TIMER_STOPPED))
                    }, RECORD_TRIGGER_TIMER_LENGTH)
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

                ACTION_TEST_ACTION -> {
                    performAction(intent.getSerializableExtra(EXTRA_ACTION) as Action)
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
        intentFilter.addAction(ACTION_TEST_ACTION)

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
                        val intent = Intent(ConfigKeymapActivity.ACTION_ADD_KEY_CHIP)
                        intent.putExtra(ConfigKeymapActivity.EXTRA_KEY_EVENT, event)

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
                    //if the Key Mapper input method isn't chosen, pass the key event to the system.
                    if (!isInputMethodChosen()) {
                        Toast.makeText(this, R.string.error_ime_must_be_chosen, Toast.LENGTH_SHORT).show()
                        return super.onKeyEvent(event)
                    }

                    mPressedTriggerKeys = mPressedKeys.toMutableList()

                    //find the keymap associated with the trigger being pressed
                    val keyMap = mKeyMapListCache.find { keyMap ->
                        keyMap.triggerList.any { trigger -> trigger.keys == mPressedTriggerKeys }
                    }

                    if (keyMap?.action != null) {
                        performAction(keyMap.action!!)
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

                //intent = null if the app doesn't exist
                if (intent != null) {
                    startActivity(intent)
                }
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
                //for actions which require the IME service
                if (action.type == ActionType.KEYCODE || action.type == ActionType.KEY) {
                    val intent = Intent(MyIMEService.ACTION_INPUT_KEYCODE)
                    //put the keycode in the intent
                    intent.putExtra(MyIMEService.EXTRA_KEYCODE, action.data.toInt())

                    sendBroadcast(intent)
                }
            }
        }
    }

    /**
     * @return whether the Key Mapper input method is chosen
     */
    private fun isInputMethodChosen(): Boolean {
        //get the current input method
        val id = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
        )

        return id.contains(packageName)
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