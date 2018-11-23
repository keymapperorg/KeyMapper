package io.github.sds100.keymapper.Services

import android.accessibilityservice.AccessibilityService
import android.content.*
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.Activities.ConfigKeymapActivity
import io.github.sds100.keymapper.Data.KeyMapRepository
import io.github.sds100.keymapper.StateChange.*
import io.github.sds100.keymapper.SystemAction.COLLAPSE_STATUS_BAR
import io.github.sds100.keymapper.SystemAction.DECREASE_BRIGHTNESS
import io.github.sds100.keymapper.SystemAction.DISABLE_AUTO_BRIGHTNESS
import io.github.sds100.keymapper.SystemAction.DISABLE_AUTO_ROTATE
import io.github.sds100.keymapper.SystemAction.DISABLE_BLUETOOTH
import io.github.sds100.keymapper.SystemAction.DISABLE_MOBILE_DATA
import io.github.sds100.keymapper.SystemAction.DISABLE_WIFI
import io.github.sds100.keymapper.SystemAction.ENABLE_AUTO_BRIGHTNESS
import io.github.sds100.keymapper.SystemAction.ENABLE_AUTO_ROTATE
import io.github.sds100.keymapper.SystemAction.ENABLE_BLUETOOTH
import io.github.sds100.keymapper.SystemAction.ENABLE_MOBILE_DATA
import io.github.sds100.keymapper.SystemAction.ENABLE_WIFI
import io.github.sds100.keymapper.SystemAction.EXPAND_NOTIFICATION_DRAWER
import io.github.sds100.keymapper.SystemAction.EXPAND_QUICK_SETTINGS
import io.github.sds100.keymapper.SystemAction.GO_BACK
import io.github.sds100.keymapper.SystemAction.GO_HOME
import io.github.sds100.keymapper.SystemAction.INCREASE_BRIGHTNESS
import io.github.sds100.keymapper.SystemAction.LANDSCAPE_MODE
import io.github.sds100.keymapper.SystemAction.NEXT_TRACK
import io.github.sds100.keymapper.SystemAction.OPEN_MENU
import io.github.sds100.keymapper.SystemAction.OPEN_RECENTS
import io.github.sds100.keymapper.SystemAction.PAUSE_MEDIA
import io.github.sds100.keymapper.SystemAction.PLAY_MEDIA
import io.github.sds100.keymapper.SystemAction.PLAY_PAUSE_MEDIA
import io.github.sds100.keymapper.SystemAction.PORTRAIT_MODE
import io.github.sds100.keymapper.SystemAction.PREVIOUS_TRACK
import io.github.sds100.keymapper.SystemAction.TOGGLE_AUTO_BRIGHTNESS
import io.github.sds100.keymapper.SystemAction.TOGGLE_AUTO_ROTATE
import io.github.sds100.keymapper.SystemAction.TOGGLE_BLUETOOTH
import io.github.sds100.keymapper.SystemAction.TOGGLE_MOBILE_DATA
import io.github.sds100.keymapper.SystemAction.TOGGLE_WIFI
import io.github.sds100.keymapper.SystemAction.VOLUME_DOWN
import io.github.sds100.keymapper.SystemAction.VOLUME_MUTE
import io.github.sds100.keymapper.SystemAction.VOLUME_SHOW_DIALOG
import io.github.sds100.keymapper.SystemAction.VOLUME_TOGGLE_MUTE
import io.github.sds100.keymapper.SystemAction.VOLUME_UNMUTE
import io.github.sds100.keymapper.SystemAction.VOLUME_UP
import io.github.sds100.keymapper.Utils.*
import org.jetbrains.anko.defaultSharedPreferences


/**
 * Created by sds100 on 16/07/2018.
 */

class MyAccessibilityService : AccessibilityService() {
    companion object {
        const val EXTRA_KEYMAP_CACHE_JSON = "extra_keymap_cache_json"
        const val EXTRA_ACTION = "action"

        const val ACTION_RECORD_TRIGGER = "${Constants.PACKAGE_NAME}.RECORD_TRIGGER"
        const val ACTION_STOP_RECORDING_TRIGGER = "${Constants.PACKAGE_NAME}.STOP_RECORDING_TRIGGER"
        const val ACTION_CLEAR_PRESSED_KEYS = "${Constants.PACKAGE_NAME}.CLEAR_PRESSED_KEYS"
        const val ACTION_UPDATE_KEYMAP_CACHE = "${Constants.PACKAGE_NAME}.UPDATE_KEYMAP_CACHE"
        const val ACTION_TEST_ACTION = "${Constants.PACKAGE_NAME}.TEST_ACTION"
        const val ACTION_RECORD_TRIGGER_TIMER_STOPPED = "${Constants.PACKAGE_NAME}.RECORD_TRIGGER_TIMER_STOPPED"

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
                    "settings put secure enabled_accessibility_services ${Constants.PACKAGE_NAME}/$className")
        }

        /**
         * Disable this accessibility service. REQUIRES ROOT
         */
        fun disableServiceInSettings() {
            RootUtils.executeRootCommand("settings put secure enabled_accessibility_services \"\"")
        }

        /**
         * @return whether the accessibility service is enabled
         */
        fun isServiceEnabled(ctx: Context): Boolean {
            /* get a list of all the enabled accessibility services.
             * The AccessibilityManager.getEnabledAccessibilityServices() method just returns an empty
             * list. :(*/
            val settingValue = Settings.Secure.getString(
                    ctx.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

            //it can be null if the user has never interacted with accessibility settings before
            if (settingValue != null) {
                return settingValue.contains(ctx.packageName)
            }

            return false
        }
    }

    private val mRecordingTimerHandler = Handler()
    private val mRecordingTimerRunnable = Runnable {
        mRecordingTrigger = false
        mPressedKeys.clear()

        sendBroadcast(Intent(ACTION_RECORD_TRIGGER_TIMER_STOPPED))
    }

    /**
     * Broadcast receiver for all intents sent from within the app.
     */
    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                ACTION_RECORD_TRIGGER -> {
                    mRecordingTrigger = true

                    //stop recording a trigger after a set amount of time.
                    mRecordingTimerHandler.postDelayed(
                            mRecordingTimerRunnable,
                            RECORD_TRIGGER_TIMER_LENGTH
                    )
                }

                ACTION_STOP_RECORDING_TRIGGER -> {
                    mRecordingTrigger = false

                    //stop the timer since the user cancelled it before the time ran out
                    mRecordingTimerHandler.removeCallbacks(mRecordingTimerRunnable)

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

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

            //are the pressed keys are trigger and if they are, is the keymap enabled
            if (isEnabledTrigger(mPressedKeys)) {

                mPressedTriggerKeys = mPressedKeys.toMutableList()

                //find the keymap associated with the trigger being pressed
                val keyMap = mKeyMapListCache.find { keyMap ->
                    keyMap.triggerList.any { trigger -> trigger.keys == mPressedTriggerKeys }
                }

                //if the keymap can't be found, pass the keyevent to the system
                if (keyMap == null) return super.onKeyEvent(event)

                if (keyMap.action != null) {
                    //if the Key Mapper input method isn't chosen, pass the key event to the system.
                    if (keyMap.action!!.requiresIME && !isInputMethodChosen()) {
                        Toast.makeText(
                                this,
                                R.string.error_ime_must_be_chosen,
                                Toast.LENGTH_SHORT
                        ).show()
                        return super.onKeyEvent(event)
                    }

                    performAction(keyMap.action!!)
                }

                return true
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
     * @return whether a key combination is registered as a trigger in the keymap cache and the keymap is enabled
     */
    private fun isEnabledTrigger(keys: MutableList<Int>): Boolean {
        return mKeyMapListCache.any { keyMap ->

            /* do any of the trigger lists for each keymap contain a trigger which matches the
             * keys being pressed?*/
            keyMap.triggerList.any { trigger ->
                trigger.keys.toTypedArray().contentEquals(keys.toTypedArray()) && keyMap.isEnabled
            }
        }
    }

    private fun performAction(action: Action) {
        //Only show a toast message that Key Mapper is performing an action if the user has enabled it
        val key = getString(R.string.key_pref_show_toast_when_action_performed)

        if (defaultSharedPreferences.getBoolean(key, false)) {
            Toast.makeText(this, R.string.performing_action, LENGTH_SHORT).show()
        }

        //if a toast message was shown that the action needs permission
        if (PermissionUtils.showPermissionWarningsForAction(this, action)) return

        when (action.type) {
            ActionType.APP -> {
                val intent = packageManager.getLaunchIntentForPackage(action.data)

                //intent = null if the app doesn't exist
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, R.string.error_app_isnt_installed, LENGTH_SHORT).show()
                }
            }

            ActionType.APP_SHORTCUT -> {
                val intent = Intent.parseUri(action.data, 0)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                try {
                    startActivity(intent)
                } catch (exception: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.error_shortcut_not_found, LENGTH_SHORT).show()
                }
            }

            ActionType.TEXT_BLOCK -> {
                val intent = Intent(MyIMEService.ACTION_INPUT_TEXT)
                //put the text in the intent
                intent.putExtra(MyIMEService.EXTRA_TEXT, action.data)

                sendBroadcast(intent)
            }

            ActionType.SYSTEM_ACTION -> performSystemAction(action.data)

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

    @Suppress("NON_EXHAUSTIVE_WHEN")
    private fun performSystemAction(@SystemAction.SystemActionId action: String) {
        when (action) {
            ENABLE_WIFI -> WifiUtils.changeWifiState(this, ENABLE)
            DISABLE_WIFI -> WifiUtils.changeWifiState(this, DISABLE)
            TOGGLE_WIFI -> WifiUtils.changeWifiState(this, TOGGLE)

            TOGGLE_BLUETOOTH -> BluetoothUtils.changeBluetoothState(TOGGLE)
            ENABLE_BLUETOOTH -> BluetoothUtils.changeBluetoothState(ENABLE)
            DISABLE_BLUETOOTH -> BluetoothUtils.changeBluetoothState(DISABLE)

            TOGGLE_MOBILE_DATA -> MobileDataUtils.toggleMobileData(this)
            ENABLE_MOBILE_DATA -> MobileDataUtils.enableMobileData()
            DISABLE_MOBILE_DATA -> MobileDataUtils.disableMobileData()

            TOGGLE_AUTO_BRIGHTNESS -> BrightnessUtils.toggleAutoBrightness(this)
            ENABLE_AUTO_BRIGHTNESS ->
                BrightnessUtils.setBrightnessMode(this, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)

            DISABLE_AUTO_BRIGHTNESS ->
                BrightnessUtils.setBrightnessMode(this, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)

            INCREASE_BRIGHTNESS -> BrightnessUtils.increaseBrightness(this)
            DECREASE_BRIGHTNESS -> BrightnessUtils.decreaseBrightness(this)

            TOGGLE_AUTO_ROTATE -> ScreenRotationUtils.toggleAutoRotate(this)
            ENABLE_AUTO_ROTATE -> ScreenRotationUtils.enableAutoRotate(this)
            DISABLE_AUTO_ROTATE -> ScreenRotationUtils.disableAutoRotate(this)
            PORTRAIT_MODE -> ScreenRotationUtils.forcePortraitMode(this)
            LANDSCAPE_MODE -> ScreenRotationUtils.forceLandscapeMode(this)

            VOLUME_UP -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_RAISE)
            VOLUME_DOWN -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_LOWER)
            VOLUME_SHOW_DIALOG -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_SAME)

            EXPAND_NOTIFICATION_DRAWER -> StatusBarUtils.expandNotificationDrawer()
            EXPAND_QUICK_SETTINGS -> StatusBarUtils.expandQuickSettings()
            COLLAPSE_STATUS_BAR -> StatusBarUtils.collapseStatusBar()

            PAUSE_MEDIA -> MediaUtils.pauseMediaPlayback(this)
            PLAY_MEDIA -> MediaUtils.playMedia(this)
            PLAY_PAUSE_MEDIA -> MediaUtils.playPauseMediaPlayback(this)
            NEXT_TRACK -> MediaUtils.nextTrack(this)
            PREVIOUS_TRACK -> MediaUtils.previousTrack(this)

            GO_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            GO_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            OPEN_RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)

            //there must be a way to do this without root
            OPEN_MENU -> RootUtils.executeRootCommand("input keyevent 82")

            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    when (action) {
                        VOLUME_UNMUTE -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_UNMUTE)
                        VOLUME_MUTE -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_MUTE)
                        VOLUME_TOGGLE_MUTE -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_TOGGLE_MUTE)
                    }
                }
            }
        }
    }

    private val KeyEvent.isVolumeKey: Boolean
        get() = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
}