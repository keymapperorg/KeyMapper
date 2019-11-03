package io.github.sds100.keymapper.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.WidgetsManager.EVENT_PAUSE_REMAPS
import io.github.sds100.keymapper.WidgetsManager.EVENT_RESUME_REMAPS
import io.github.sds100.keymapper.WidgetsManager.EVENT_SERVICE_START
import io.github.sds100.keymapper.WidgetsManager.EVENT_SERVICE_STOPPED
import io.github.sds100.keymapper.activity.ConfigKeymapActivity
import io.github.sds100.keymapper.data.AppDatabase
import io.github.sds100.keymapper.delegate.ActionPerformerDelegate
import io.github.sds100.keymapper.interfaces.IContext
import io.github.sds100.keymapper.interfaces.IPerformAccessibilityAction
import io.github.sds100.keymapper.util.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.toast

/**
 * Created by sds100 on 16/07/2018.
 */

class MyAccessibilityService : AccessibilityService(), IContext, IPerformAccessibilityAction, LifecycleOwner {

    companion object {
        const val EXTRA_KEYMAP_CACHE_JSON = "extra_keymap_cache_json"
        const val EXTRA_ACTION = "action"
        const val EXTRA_TIME_LEFT = "time"

        const val ACTION_RECORD_TRIGGER = "$PACKAGE_NAME.RECORD_TRIGGER"
        const val ACTION_CLEAR_PRESSED_KEYS = "$PACKAGE_NAME.CLEAR_PRESSED_KEYS"
        const val ACTION_UPDATE_KEYMAP_CACHE = "$PACKAGE_NAME.UPDATE_KEYMAP_CACHE"
        const val ACTION_TEST_ACTION = "$PACKAGE_NAME.TEST_ACTION"
        const val ACTION_STOP_RECORDING_TRIGGER = "$PACKAGE_NAME.STOP_RECORDING_TRIGGER"
        const val ACTION_RECORD_TRIGGER_TIMER_INCREMENTED = "$PACKAGE_NAME.TIMER_INCREMENTED"
        const val ACTION_PAUSE_REMAPPINGS = "$PACKAGE_NAME.PAUSE_REMAPPINGS"
        const val ACTION_RESUME_REMAPPINGS = "$PACKAGE_NAME.RESUME_REMAPPINGS"
        const val ACTION_UPDATE_NOTIFICATION = "$PACKAGE_NAME.UPDATE_NOTIFICATION"
        const val ACTION_START = "$PACKAGE_NAME.START_ACCESSIBILITY_SERVICE"
        const val ACTION_STOP = "$PACKAGE_NAME.STOP_ACCESSIBILITY_SERVICE"
        const val ACTION_ON_START = "$PACKAGE_NAME.ON_START_ACCESSIBILITY_SERVICE"
        const val ACTION_ON_STOP = "$PACKAGE_NAME.ON_STOP_ACCESSIBILITY_SERVICE"
        const val ACTION_SHOW_KEYBOARD = "$PACKAGE_NAME.SHOW_KEYBOARD"

        /**
         * How long should the accessibility service record a trigger in ms.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5000L

        private const val RECORD_TRIGGER_TIMER_INCREMENT = 1000L

        /**
         * The time in ms between repeating an action while holding down.
         */
        private const val REPEAT_DELAY = 50L

        /**
         * How long a key should be held down to repeatedly perform an action in ms.
         */
        private const val HOLD_DOWN_DELAY = 400L

        /**
         * Some keys need to be consumed on the up event to prevent them from working they way they are intended to.
         */
        private val KEYS_TO_CONSUME_UP_EVENT = listOf(
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH
        )
    }

    /**
     * How long a long-press is in ms.
     */
    private val mLongPressDelay
        get() = ctx.defaultSharedPreferences.getInt(
            ctx.str(R.string.key_pref_long_press_delay),
            ctx.int(R.integer.default_value_long_press_delay)).toLong()

    private var mPaused = false

    private val mRecordTriggerRunnable = object : Runnable {
        var timeLeft = RECORD_TRIGGER_TIMER_LENGTH

        override fun run() {
            if (timeLeft == 0L) {
                sendBroadcast(ACTION_STOP_RECORDING_TRIGGER)
                Logger.write(ctx, "Stopped Recording", "Stopped recording a trigger")
                mHandler.removeCallbacks(this)

                mRecordingTrigger = false
                mPressedKeys.clear()

                //reset the timer for the next time it is run
                timeLeft = RECORD_TRIGGER_TIMER_LENGTH
                return
            }

            Intent(ACTION_RECORD_TRIGGER_TIMER_INCREMENTED).apply {
                putExtra(EXTRA_TIME_LEFT, timeLeft)

                sendBroadcast(this)
            }

            timeLeft -= RECORD_TRIGGER_TIMER_INCREMENT

            mHandler.postDelayed(this, RECORD_TRIGGER_TIMER_INCREMENT)
        }
    }

    /**
     * Broadcast receiver for all intents sent from within the app.
     */
    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_RECORD_TRIGGER -> {
                    mRecordingTrigger = true

                    mHandler.post(mRecordTriggerRunnable)
                    Logger.write(ctx, "Recording", "Started recording a trigger")
                }

                ACTION_CLEAR_PRESSED_KEYS -> {
                    mPressedKeys.clear()
                }

                ACTION_UPDATE_KEYMAP_CACHE -> {
                    //deserialize the keymap list
                    val jsonString = intent.getStringExtra(EXTRA_KEYMAP_CACHE_JSON)

                    if (jsonString != null) {
                        /* app can crash if it can't deserialize the JSON. I don't know how to handle the try-catch in a
                        * meaningful way */
                        try {
                            mKeyMapListCache = Gson().fromJson(jsonString)
                        } catch (e: Exception) {
                        }
                    }
                }

                ACTION_TEST_ACTION -> {
                    intent.getSerializableExtra(EXTRA_ACTION)?.let { action ->
                        mActionPerformerDelegate.performAction(
                            action = action as Action,
                            flags = 0x0)
                    }
                }

                ACTION_PAUSE_REMAPPINGS -> {
                    clearLists()
                    mPaused = true
                    WidgetsManager.onEvent(ctx, EVENT_PAUSE_REMAPS)
                }

                ACTION_RESUME_REMAPPINGS -> {
                    clearLists()
                    mPaused = false
                    WidgetsManager.onEvent(ctx, EVENT_RESUME_REMAPS)
                }

                ACTION_UPDATE_NOTIFICATION -> {
                    if (mPaused) {
                        WidgetsManager.onEvent(ctx, EVENT_PAUSE_REMAPS)
                    } else {
                        WidgetsManager.onEvent(ctx, EVENT_RESUME_REMAPS)
                    }
                }

                Intent.ACTION_SCREEN_ON -> {
                    clearLists()
                }

                ACTION_SHOW_KEYBOARD ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        softKeyboardController.show(ctx)
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
    private val mPressedKeys = mutableSetOf<Int>()

    /**
     * When all the keys that map to a specific trigger are pressed, they are put in here.
     * E.g when Ctrl + J is pressed, the contents of this list will be the keycodes for Ctrl + J
     */
    private var mPressedTriggerKeys = mutableListOf<Int>()

    /**
     * The triggers of any long press actions waiting to be performed. This is to let short press actions, with the same
     * trigger as a long press, to know whether to perform when they are pressed down or ONLY if the key is released
     * before the long press delay.
     */
    private val mTriggersAwaitingLongPress = mutableListOf<Trigger>()

    private val mHandler = Handler()

    /* How does long pressing work?
    Any short press actions, which have the same trigger as a long press action, should be performed if the key is
    released before the long press delay.

       - When a trigger is detected, a Runnable is created, which when executed, will perform the action.
       - The mRecordTriggerRunnable will be queued in the Handler.
       - After the long press delay the mRecordTriggerRunnable will be executed if it is still queued in the Handler.
       - If the user releases one of the keys which is assigned to the Runnable, the Runnable
       will be removed from the Runnable list and removed from the Handler. This stops it being executed after the user
       has stopped long-pressing the trigger.
       -
    * */
    private val mLongPressPendingActions = mutableListOf<PendingAction>()
    private val mShortPressPendingActions = mutableListOf<PendingAction>()
    private val mRepeatQueue = mutableListOf<PendingAction>()

    /**
     * Some keys need to be consumed on the up event to prevent them from working they way they are intended to.
     */
    private val mKeysToConsumeOnUp = mutableSetOf<Int>()

    private var mRecordingTrigger = false

    private lateinit var mActionPerformerDelegate: ActionPerformerDelegate

    private lateinit var mLifecycleRegistry: LifecycleRegistry

    override val ctx: Context
        get() = this

    override val keyboardController: SoftKeyboardController?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            softKeyboardController
        } else {
            null
        }

    override val rootNode: AccessibilityNodeInfo?
        get() = rootInActiveWindow

    override fun getLifecycle() = mLifecycleRegistry

    override fun onServiceConnected() {
        super.onServiceConnected()

        mLifecycleRegistry = LifecycleRegistry(this)
        mLifecycleRegistry.currentState = Lifecycle.State.STARTED

        mActionPerformerDelegate = ActionPerformerDelegate(
            iContext = this, iPerformAccessibilityAction = this, lifecycle = lifecycle)

        //listen for events from NewKeymapActivity
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_CLEAR_PRESSED_KEYS)
        intentFilter.addAction(ACTION_UPDATE_KEYMAP_CACHE)
        intentFilter.addAction(ACTION_TEST_ACTION)
        intentFilter.addAction(ACTION_PAUSE_REMAPPINGS)
        intentFilter.addAction(ACTION_RESUME_REMAPPINGS)
        intentFilter.addAction(ACTION_UPDATE_NOTIFICATION)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(ACTION_RECORD_TRIGGER)
        intentFilter.addAction(ACTION_SHOW_KEYBOARD)

        registerReceiver(mBroadcastReceiver, intentFilter)

        getKeyMapList()

        WidgetsManager.onEvent(ctx, EVENT_SERVICE_START)
        sendBroadcast(Intent(ACTION_ON_START))

        Logger.write(ctx, title = "Service Started", message = "Accessibility Service started")
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        WidgetsManager.onEvent(ctx, EVENT_SERVICE_STOPPED)

        mLifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        unregisterReceiver(mBroadcastReceiver)
        sendBroadcast(Intent(ACTION_ON_STOP))

        Logger.write(ctx, title = "Service Destroyed", message = "Accessibility Service destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        when {
            event?.action == KeyEvent.ACTION_DOWN -> Logger.write(ctx, "Down Key Event", event.toString())
            event?.action == KeyEvent.ACTION_UP -> Logger.write(ctx, "Up Key Event", event.toString())
            else -> Logger.write(ctx, "Other Key Event", event.toString())
        }

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

                /*only execute short press actions with the same trigger as a long press if the key has been held down
                * shorter than the long press delay */
                if (SystemClock.uptimeMillis() - event.downTime < mLongPressDelay) {
                    var performedShortPress = false

                    mShortPressPendingActions.filter { it.trigger.keys.contains(event.keyCode) }.forEach {
                        performedShortPress = true
                        it.run()
                        mHandler.removeCallbacks(it)
                        mShortPressPendingActions.remove(it)
                    }

                    /*only imitate a short press for a button if the user hasn't mapped it to a short press action */
                    if (!performedShortPress &&
                        mLongPressPendingActions.any { it.trigger.keys.contains(event.keyCode) }) {
                        imitateButtonPress(event.keyCode)
                    }
                } else {
                    /* must remove all short press pending actions created during the down press. But don't run them
                    * since only the long press should be ran. */
                    mShortPressPendingActions.filter { it.trigger.keys.contains(event.keyCode) }.forEach {
                        mHandler.removeCallbacks(it)
                        mShortPressPendingActions.remove(it)
                    }
                }

                //remove all pending long press actions since their trigger has been released
                mLongPressPendingActions.filter { it.trigger.keys.contains(event.keyCode) }.forEach {
                    mHandler.removeCallbacks(it)
                    mTriggersAwaitingLongPress.remove(it.trigger)
                    mLongPressPendingActions.remove(it)
                }

                //remove all pending actions to repeat since their trigger has been released
                mRepeatQueue.filter { it.trigger.keys.contains(event.keyCode) }.forEach {
                    mHandler.removeCallbacks(it)
                    mRepeatQueue.remove(it)
                }

                if (mKeysToConsumeOnUp.contains(event.keyCode)) {
                    mKeysToConsumeOnUp.remove(event.keyCode)
                    return true
                }

                return super.onKeyEvent(event)
            }

            mPressedTriggerKeys = mPressedKeys.toMutableList()

            var consumeEvent = false

            //find all the keymap which can be triggered with the keys being pressed
            val keyMaps = mutableListOf<KeyMap>()

            mKeyMapListCache.forEach { keymap ->
                /* only add a trigger to the list if it hasn't already been registered as a long press trigger */
                if (keymap.isLongPress && keymap.isEnabled) {
                    val newLongPressTriggers = keymap.triggerList.filter { !mTriggersAwaitingLongPress.contains(it) }

                    mTriggersAwaitingLongPress.addAll(newLongPressTriggers)
                }

                if (keymap.containsTrigger(mPressedTriggerKeys) && keymap.isEnabled) {
                    keyMaps.add(keymap)
                }
            }

            //if no applicable keymaps are found the keyevent won't be consumed
            if (keyMaps.isEmpty()) return super.onKeyEvent(event)

            //loop through each keymap and perform their action
            for (keymap in keyMaps) {
                val errorResult = ActionUtils.getError(this, keymap.action)

                //if there is no error
                if (errorResult != null) {
                    val errorDescription = ErrorCodeUtils.getErrorCodeDescription(this, errorResult)

                    toast(errorDescription)
                    continue
                }

                val trigger = Trigger(mPressedTriggerKeys)

                //if the action should only be performed if it is a long press
                if (keymap.isLongPress) {

                    val runnable = object : PendingAction(trigger) {
                        override fun run() {
                            performAction(keymap.action!!, keymap.flags)
                        }
                    }

                    mLongPressPendingActions.add(runnable)
                    mHandler.postDelayed(runnable, mLongPressDelay)

                    consumeEvent = true
                    continue

                } else {
                    //if a short press click

                    /* if there is a long press keymap with the same trigger as this short press one, only perform it
                    * if the trigger is released before the long press delay. Therefore, a Runnable is created which
                    * will be ran later. */
                    if (mTriggersAwaitingLongPress.any { it == trigger }) {
                        val runnable = object : PendingAction(trigger) {
                            override fun run() {
                                performAction(keymap.action!!, keymap.flags)
                            }
                        }

                        mShortPressPendingActions.add(runnable)
                        consumeEvent = true
                        continue

                    } else {
                        if (keymap.action!!.isVolumeAction
                            || keymap.action!!.type == ActionType.KEY
                            || keymap.action!!.type == ActionType.KEYCODE) {

                            val runnable = object : PendingAction(trigger) {
                                var flags = removeFlag(keymap.flags, FlagUtils.FLAG_VIBRATE)

                                override fun run() {
                                    performAction(keymap.action!!, flags)
                                    mHandler.postDelayed(this, REPEAT_DELAY)
                                }
                            }

                            mRepeatQueue.add(runnable)
                            mHandler.postDelayed(runnable, HOLD_DOWN_DELAY)
                        }

                        performAction(keymap.action!!, keymap.flags)

                        consumeEvent = true
                    }
                }
            }

            if (consumeEvent) {
                logConsumedKeyEvent(event)
                return true
            }

        } catch (e: Exception) {
            Logger.write(ctx,
                isError = true,
                title = "Exception in onKeyEvent()",
                message = e.stackTrace.toString())

            if (BuildConfig.DEBUG) {
                Log.e(this::class.java.simpleName, "ONKEYEVENT CRASH")
                e.printStackTrace()
            }
        }

        return super.onKeyEvent(event)
    }

    private fun performAction(action: Action, flags: Int) {
        mActionPerformerDelegate.performAction(action, flags)

        Logger.write(ctx, "Performed Action", "${action.type} ${action.data} ${action.extras}")

        mPressedTriggerKeys.forEach {
            if (KEYS_TO_CONSUME_UP_EVENT.contains(it)) {
                mKeysToConsumeOnUp.add(it)
            }
        }
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
            KeyEvent.KEYCODE_VOLUME_UP -> AudioUtils.adjustVolume(ctx, AudioManager.ADJUST_RAISE,
                showVolumeUi = true)

            KeyEvent.KEYCODE_VOLUME_DOWN -> AudioUtils.adjustVolume(ctx, AudioManager.ADJUST_LOWER,
                showVolumeUi = true)

            KeyEvent.KEYCODE_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            KeyEvent.KEYCODE_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            KeyEvent.KEYCODE_APP_SWITCH -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            KeyEvent.KEYCODE_MENU -> mActionPerformerDelegate.performSystemAction(SystemAction.OPEN_MENU)
        }
    }

    private fun clearLists() {
        mPressedKeys.clear()
        mPressedTriggerKeys.clear()
        mTriggersAwaitingLongPress.clear()
        mLongPressPendingActions.clear()
        mShortPressPendingActions.clear()
        mRepeatQueue.clear()
    }

    private fun logConsumedKeyEvent(event: KeyEvent) {
        if (event.action == KeyEvent.ACTION_DOWN) {
            Logger.write(ctx, "Consumed Down", event.toString())
        } else if (event.action == KeyEvent.ACTION_UP) {
            Logger.write(ctx, "Consumed Up", event.toString())
        }

        Log.i(this::class.java.simpleName, "Consumed key event ${event.keyCode} ${event.action}")
    }
}