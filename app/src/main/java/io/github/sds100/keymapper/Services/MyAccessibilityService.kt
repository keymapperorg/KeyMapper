package io.github.sds100.keymapper.Services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.provider.Settings
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
import io.github.sds100.keymapper.Activities.ConfigKeymapActivity
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.Data.KeyMapRepository
import io.github.sds100.keymapper.Delegates.ActionPerformerDelegate
import io.github.sds100.keymapper.Interfaces.IContext
import io.github.sds100.keymapper.Interfaces.IPerformGlobalAction
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.Utils.ActionUtils
import io.github.sds100.keymapper.Utils.ErrorCodeUtils
import io.github.sds100.keymapper.Utils.FlagUtils.FLAG_LONG_PRESS
import io.github.sds100.keymapper.Utils.RootUtils
import io.github.sds100.keymapper.Utils.isVolumeKey

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
         * How long a long-press is.
         */
        private const val LONG_PRESS_DELAY = 500L

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
                return settingValue.contains(ctx.packageName)
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
                            intent.getSerializableExtra(EXTRA_ACTION) as Action,
                            listOf())
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

                mLongPressRunnables.filter { it.trigger.contains(event.keyCode) }.forEach {
                    mHandler.removeCallbacks(it)
                    mLongPressRunnables.remove(it)
                }

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
                    if (keyMap.flags.contains(FLAG_LONG_PRESS)) {

                        val runnable = Runnable {
                            mActionPerformerDelegate.performAction(keyMap.action!!, keyMap.flags)
                        }

                        runnable.trigger = mPressedTriggerKeys

                        mLongPressRunnables.add(runnable)

                        mHandler.postDelayed(runnable, LONG_PRESS_DELAY)

                    } else {
                        mActionPerformerDelegate.performAction(keyMap.action!!, keyMap.flags)
                    }
                } else {
                    val errorDescription = ErrorCodeUtils.getErrorCodeDescription(this, errorResult)

                    Toast.makeText(this, errorDescription, LENGTH_SHORT).show()
                }
            }

            return true
        }
    }

    private fun getKeyMapListFromRepository() {
        val list = KeyMapRepository.getInstance(baseContext).keyMapList.value

        if (list != null) {
            mKeyMapListCache = list
        }
    }
}