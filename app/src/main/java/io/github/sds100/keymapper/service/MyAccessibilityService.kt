package io.github.sds100.keymapper.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.activity.ConfigKeymapActivity
import io.github.sds100.keymapper.data.KeyMapRepository
import io.github.sds100.keymapper.delegate.ActionPerformerDelegate
import io.github.sds100.keymapper.interfaces.IContext
import io.github.sds100.keymapper.interfaces.IPerformGlobalAction
import io.github.sds100.keymapper.isVolumeAction
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.FlagUtils.FLAG_LONG_PRESS

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

        /**
         * How long should the accessibility service record a trigger. In milliseconds.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5000L

        /**
         * How long a long-press is in ms.
         */
        private const val LONG_PRESS_DELAY = 500L

        /**
         * The time in ms between repeating an action while holding down.
         */
        private const val REPEAT_DELAY = 10L

        /**
         * How long a key should be held down to repeatedly perform an action in ms.
         */
        private const val HOLD_DOWN_DELAY = 200L

        /**
         * Enable this accessibility service. REQUIRES ROOT
         */
        fun enableServiceInSettings() {
            val className = MyAccessibilityService::class.java.name

            RootUtils.changeSecureSetting("enabled_accessibility_services", "$PACKAGE_NAME/$className")
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

    override val ctx
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
    private val mRunnables = mutableListOf<Runnable>()

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
        mLifecycleRegistry.markState(Lifecycle.State.CREATED)

        mActionPerformerDelegate = ActionPerformerDelegate(
                iContext = this, iPerformGlobalAction = this, lifecycle = lifecycle)

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

        mLifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

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


            //when a key is pressed down
            if (event.action == KeyEvent.ACTION_DOWN) {
                mPressedKeys.add(event.keyCode)

                //when a key is lifted
            } else if (event.action == KeyEvent.ACTION_UP) {

                mPressedKeys.remove(event.keyCode)

                mRunnables.filter { it.trigger.contains(event.keyCode) }.forEach {
                    mHandler.removeCallbacks(it)
                    mRunnables.remove(it)
                }

                if (mPressedTriggerKeys.isNotEmpty()) {
                    if (mPressedTriggerKeys.contains(event.keyCode)) {
                        mPressedTriggerKeys.remove(event.keyCode)

                        /* pass the volume key event to the system otherwise strange behaviour
                        * happens where the volume key is constantly being pressed */
                        if (event.isVolumeKey) {
                            return super.onKeyEvent(event)
                        }

                        logConsumedKeyEvent(event)
                        return true
                    }
                }
            }

            mPressedTriggerKeys = mPressedKeys.toMutableList()

            //find all the keymap which can be triggered with the keys being pressed
            val keyMaps = mKeyMapListCache.filter { keyMap ->
                keyMap.triggerList.any { trigger ->
                    trigger.keys.toTypedArray().contentEquals(mPressedKeys.toTypedArray()) && keyMap.isEnabled
                }
            }

            //if no applicable keymaps are found the keyevent won't be consumed
            if (keyMaps.isEmpty()) return super.onKeyEvent(event)

            //loop through each keymap and perform their action
            keyMaps.forEach { keyMap ->
                val errorResult = ActionUtils.getPotentialErrorCode(this, keyMap.action)

                //if there is no error
                if (errorResult == null) {
                    //if the action should only be performed if it is a long press
                    if (containsFlag(keyMap.flags, FLAG_LONG_PRESS)) {

                        val runnable = Runnable {
                            mActionPerformerDelegate.performAction(keyMap.action!!, keyMap.flags)
                        }

                        runnable.trigger = mPressedTriggerKeys

                        mRunnables.add(runnable)
                        mHandler.postDelayed(runnable, LONG_PRESS_DELAY)

                        return super.onKeyEvent(event)

                        //for example, you dont want an app or app-shortcut to be repeatedly opened.
                    } else if (keyMap.action!!.isVolumeAction
                            || keyMap.action!!.type == ActionType.KEY
                            || keyMap.action!!.type == ActionType.KEYCODE) {

                        val runnable = object : Runnable {
                            private var mShouldRepeat = false

                            override fun run() {
                                mActionPerformerDelegate.performAction(keyMap.action!!, keyMap.flags)

                                if (mShouldRepeat) {
                                    mHandler.postDelayed(this, REPEAT_DELAY)
                                } else {
                                    //wait a bit before registering the key as being held down.
                                    mHandler.postDelayed(this, HOLD_DOWN_DELAY)
                                    mShouldRepeat = true
                                }
                            }
                        }

                        runnable.trigger = mPressedTriggerKeys

                        mRunnables.add(runnable)
                        mHandler.post(runnable)
                    } else {
                        mActionPerformerDelegate.performAction(keyMap.action!!, keyMap.flags)
                    }

                } else {
                    val errorDescription = ErrorCodeUtils.getErrorCodeDescription(this, errorResult)

                    Toast.makeText(this, errorDescription, LENGTH_SHORT).show()
                }
            }

            logConsumedKeyEvent(event)
            return true
        }
    }

    private fun getKeyMapListFromRepository() {
        val list = KeyMapRepository.getInstance(baseContext).keyMapList.value

        if (list != null) {
            mKeyMapListCache = list
        }
    }

    private fun logConsumedKeyEvent(event: KeyEvent) {
        Log.i(this::class.java.simpleName, "Consumed key event ${event.keyCode} ${event.action}")
    }
}