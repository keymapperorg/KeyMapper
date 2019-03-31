package io.github.sds100.keymapper.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import com.crashlytics.android.Crashlytics
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.AccessibilityServiceWidgetsManager.EVENT_PAUSE_REMAPS
import io.github.sds100.keymapper.AccessibilityServiceWidgetsManager.EVENT_RESUME_REMAPS
import io.github.sds100.keymapper.AccessibilityServiceWidgetsManager.EVENT_SERVICE_START
import io.github.sds100.keymapper.AccessibilityServiceWidgetsManager.EVENT_SERVICE_STOPPED
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.activity.ConfigKeymapActivity
import io.github.sds100.keymapper.data.AppDatabase
import io.github.sds100.keymapper.delegate.ActionPerformerDelegate
import io.github.sds100.keymapper.interfaces.IContext
import io.github.sds100.keymapper.interfaces.IPerformGlobalAction
import io.github.sds100.keymapper.util.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.toast

/**
 * Created by sds100 on 16/07/2018.
 */

class MyAccessibilityService : AccessibilityService(), IContext, IPerformGlobalAction, LifecycleOwner {

    companion object {
        const val EXTRA_KEYMAP_CACHE_JSON = "extra_keymap_cache_json"
        const val EXTRA_ACTION = "action"

        const val ACTION_RECORD_TRIGGER = "$PACKAGE_NAME.RECORD_TRIGGER"
        const val ACTION_STOP_RECORDING_TRIGGER = "$PACKAGE_NAME.STOP_RECORDING_TRIGGER"
        const val ACTION_CLEAR_PRESSED_KEYS = "$PACKAGE_NAME.CLEAR_PRESSED_KEYS"
        const val ACTION_UPDATE_KEYMAP_CACHE = "$PACKAGE_NAME.UPDATE_KEYMAP_CACHE"
        const val ACTION_TEST_ACTION = "$PACKAGE_NAME.TEST_ACTION"
        const val ACTION_RECORD_TRIGGER_TIMER_STOPPED = "$PACKAGE_NAME.RECORD_TRIGGER_TIMER_STOPPED"
        const val ACTION_PAUSE_REMAPPINGS = "$PACKAGE_NAME.PAUSE_REMAPPINGS"
        const val ACTION_RESUME_REMAPPINGS = "$PACKAGE_NAME.RESUME_REMAPPINGS"
        const val ACTION_UPDATE_NOTIFICATION = "$PACKAGE_NAME.UPDATE_NOTIFICATION"
        const val ACTION_START = "$PACKAGE_NAME.START_ACCESSIBILITY_SERVICE"
        const val ACTION_STOP = "$PACKAGE_NAME.STOP_ACCESSIBILITY_SERVICE"
        const val ACTION_ON_START = "$PACKAGE_NAME.ON_START_ACCESSIBILITY_SERVICE"
        const val ACTION_ON_STOP = "$PACKAGE_NAME.ON_STOP_ACCESSIBILITY_SERVICE"

        /**
         * How long should the accessibility service record a trigger. In milliseconds.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5000L

        /**
         * The time in ms between repeating an action while holding down.
         */
        private const val REPEAT_DELAY = 10L

        /**
         * How long a key should be held down to repeatedly perform an action in ms.
         */
        private const val HOLD_DOWN_DELAY = 400L

        /**
         * Enable this accessibility service. REQUIRES ROOT
         */
        fun enableServiceInSettingsRoot() {
            val className = MyAccessibilityService::class.java.name

            RootUtils.changeSecureSetting("enabled_accessibility_services", "$PACKAGE_NAME/$className")
        }

        /**
         * Disable this accessibility service. REQUIRES ROOT
         */
        fun disableServiceInSettingsRoot() {
            RootUtils.executeRootCommand("settings put secure enabled_accessibility_services \"\"")
        }

        fun openAccessibilitySettings(ctx: Context) {
            try {
                val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                ctx.startActivity(settingsIntent)
            } catch (e: Exception) {
                ctx.toast(R.string.error_cant_find_accessibility_settings_page)
            }
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
                /* cant just use .contains because the debug and release accessibility service both contain
                   io.github.sds100.keymapper. the enabled_accessibility_services are stored as

                     io.github.sds100.keymapper.debug/io.github.sds100.keymapper.service.MyAccessibilityService
                     :io.github.sds100.keymapper/io.github.sds100.keymapper.service.MyAccessibilityService

                     without the new line before the :
                */
                return settingValue.split(':').any { it.split('/')[0] == ctx.packageName }
            }

            return false
        }
    }

    /**
     * How long a long-press is in ms.
     */
    private val LONG_PRESS_DELAY
        get() = ctx.defaultSharedPreferences.getInt(
                ctx.str(R.string.key_pref_long_press_delay),
                ctx.int(R.integer.default_value_long_press_delay)).toLong()

    private var mPaused = false

    override val ctx: Context
        get() = this

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
                    mHandler.postDelayed(
                            mRecordingTimerRunnable,
                            RECORD_TRIGGER_TIMER_LENGTH
                    )
                }

                ACTION_STOP_RECORDING_TRIGGER -> {
                    mRecordingTrigger = false

                    //stop the timer since the user cancelled it before the time ran out
                    mHandler.removeCallbacks(mRecordingTimerRunnable)

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
                    mActionPerformerDelegate.performAction(
                            action = intent.getSerializableExtra(EXTRA_ACTION) as Action,
                            flags = 0x0)
                }

                ACTION_PAUSE_REMAPPINGS -> {
                    mPaused = true
                    AccessibilityServiceWidgetsManager.onEvent(ctx, EVENT_PAUSE_REMAPS)
                }

                ACTION_RESUME_REMAPPINGS -> {
                    mPaused = false
                    AccessibilityServiceWidgetsManager.onEvent(ctx, EVENT_RESUME_REMAPS)
                }

                ACTION_UPDATE_NOTIFICATION -> {
                    if (mPaused) {
                        AccessibilityServiceWidgetsManager.onEvent(ctx, EVENT_PAUSE_REMAPS)
                    } else {
                        AccessibilityServiceWidgetsManager.onEvent(ctx, EVENT_RESUME_REMAPS)
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

    private val mHandler = Handler()

    /* How does long pressing work?
       - When a trigger is detected, a Runnable is created which when executed will perform the action.
       - The runnable will be queued in the Handler.
       - After 500ms the runnable will be executed if it is still queued in the Handler.
       - If the user releases one of the keys which is assigned to a Runnable in the mRunnableTriggerMap, the Runnable
       will be removed from the Runnable list and removed from the Handler. This stops it being executed after the user
       has stopped long-pressing the trigger.
    * */
    private val mLongPressRunnables = mutableListOf<Runnable>()
    private val mRepeatRunnables = mutableListOf<Runnable>()

    private val mRunnableTriggerMap = mutableMapOf<Int, List<Int>>()

    private var Runnable.trigger: List<Int>
        get() = mRunnableTriggerMap.getValue(hashCode())
        set(value) {
            mRunnableTriggerMap[hashCode()] = value
        }

    private var mRecordingTrigger = false

    private lateinit var mActionPerformerDelegate: ActionPerformerDelegate

    private lateinit var mLifecycleRegistry: LifecycleRegistry

    override fun getLifecycle() = mLifecycleRegistry

    override fun onServiceConnected() {
        super.onServiceConnected()

        mLifecycleRegistry = LifecycleRegistry(this)
        mLifecycleRegistry.currentState = Lifecycle.State.STARTED

        mActionPerformerDelegate = ActionPerformerDelegate(
                iContext = this, iPerformGlobalAction = this, lifecycle = lifecycle)

        //listen for events from NewKeymapActivity
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RECORD_TRIGGER)
        intentFilter.addAction(ACTION_STOP_RECORDING_TRIGGER)
        intentFilter.addAction(ACTION_CLEAR_PRESSED_KEYS)
        intentFilter.addAction(ACTION_UPDATE_KEYMAP_CACHE)
        intentFilter.addAction(ACTION_TEST_ACTION)
        intentFilter.addAction(ACTION_PAUSE_REMAPPINGS)
        intentFilter.addAction(ACTION_RESUME_REMAPPINGS)
        intentFilter.addAction(ACTION_UPDATE_NOTIFICATION)

        registerReceiver(mBroadcastReceiver, intentFilter)

        getKeyMapList()

        AccessibilityServiceWidgetsManager.onEvent(ctx, EVENT_SERVICE_START)
        sendBroadcast(Intent(ACTION_ON_START))
    }


    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        AccessibilityServiceWidgetsManager.onEvent(ctx, EVENT_SERVICE_STOPPED)

        mLifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        unregisterReceiver(mBroadcastReceiver)
        sendBroadcast(Intent(ACTION_ON_STOP))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

        try {
            if (mRecordingTrigger) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    //tell NewKeymapActivity to add the chip
                    val intent = Intent(ConfigKeymapActivity.ACTION_ADD_KEY_CHIP)
                    intent.putExtra(ConfigKeymapActivity.EXTRA_KEY_EVENT, event)

                    sendBroadcast(intent)
                }

                logConsumedKeyEvent(event)
                //Don't allow the key to do anything when recording a trigger
                return true
            }

            if (mPaused) return super.onKeyEvent(event)

            //when a key is pressed down
            if (event.action == KeyEvent.ACTION_DOWN) {
                mPressedKeys.add(event.keyCode)

                //when a key is lifted
            } else if (event.action == KeyEvent.ACTION_UP) {

                mPressedKeys.remove(event.keyCode)

                if (mLongPressRunnables.any { it.trigger.contains(event.keyCode) }) {
                    imitateButtonPress(event.keyCode)
                }

                mLongPressRunnables.forEach {
                    if (it.trigger.contains(event.keyCode)) {
                        mHandler.removeCallbacks(it)
                        mLongPressRunnables.remove(it)
                    }
                }

                mRepeatRunnables.forEach {
                    if (it.trigger.contains(event.keyCode)) {
                        mHandler.removeCallbacks(it)
                        mRepeatRunnables.remove(it)
                    }
                }
            }

            mPressedTriggerKeys = mPressedKeys.toMutableList()

            //find all the keymap which can be triggered with the keys being pressed
            val keyMaps = mKeyMapListCache.filter { keymap ->
                keymap.containsTrigger(mPressedTriggerKeys) && keymap.isEnabled
            }

            //if no applicable keymaps are found the keyevent won't be consumed
            if (keyMaps.isEmpty()) return super.onKeyEvent(event)

            var consumeEvent = false

            //loop through each keymap and perform their action
            for (keymap in keyMaps) {
                val errorResult = ActionUtils.getPotentialErrorCode(this, keymap.action)

                //if there is no error
                if (errorResult != null) {
                    val errorDescription = ErrorCodeUtils.getErrorCodeDescription(this, errorResult)

                    Toast.makeText(this, errorDescription, LENGTH_SHORT).show()
                    continue
                }

                //if the action should only be performed if it is a long press
                if (keymap.isLongPress) {

                    val runnable = object : Runnable {
                        override fun run() {

                            mActionPerformerDelegate.performAction(keymap.action!!, keymap.flags)

                            mRunnableTriggerMap.remove(this.hashCode())
                            mLongPressRunnables.remove(this)
                        }
                    }

                    runnable.trigger = mPressedTriggerKeys

                    mLongPressRunnables.add(runnable)
                    mHandler.postDelayed(runnable, LONG_PRESS_DELAY)

                    consumeEvent = true
                    continue
                }

                if (keymap.action!!.isVolumeAction
                        || keymap.action!!.type == ActionType.KEY
                        || keymap.action!!.type == ActionType.KEYCODE) {

                    val runnable = object : Runnable {
                        override fun run() {
                            mActionPerformerDelegate.performAction(keymap.action!!, keymap.flags)

                            mHandler.postDelayed(this, REPEAT_DELAY)
                        }
                    }

                    runnable.trigger = mPressedTriggerKeys

                    mRepeatRunnables.add(runnable)
                    mHandler.postDelayed(runnable, HOLD_DOWN_DELAY)
                }

                mActionPerformerDelegate.performAction(keymap.action!!, keymap.flags)
                consumeEvent = true
            }

            if (consumeEvent) {
                logConsumedKeyEvent(event)
                return true
            }
        } catch (e: Exception) {

            if (BuildConfig.DEBUG) {
                Toast.makeText(this, R.string.exception_accessibility_service, LENGTH_SHORT).show()
            }

            Crashlytics.logException(e)
        }

        return super.onKeyEvent(event)
    }

    private fun getKeyMapList() {
        AppDatabase.getInstance(this).keyMapDao().getAll().observe(this, Observer {
            if (it != null) {
                mKeyMapListCache = it
            }
        })
    }

    private fun imitateButtonPress(keyCode: Int) {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> VolumeUtils.adjustVolume(ctx, AudioManager.ADJUST_RAISE,
                    showVolumeUi = true)

            KeyEvent.KEYCODE_VOLUME_DOWN -> VolumeUtils.adjustVolume(ctx, AudioManager.ADJUST_LOWER,
                    showVolumeUi = true)

            KeyEvent.KEYCODE_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            KeyEvent.KEYCODE_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            KeyEvent.KEYCODE_APP_SWITCH -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            KeyEvent.KEYCODE_MENU -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        }
    }

    private fun logConsumedKeyEvent(event: KeyEvent) {
        Log.i(this::class.java.simpleName, "Consumed key event ${event.keyCode} ${event.action}")
    }
}