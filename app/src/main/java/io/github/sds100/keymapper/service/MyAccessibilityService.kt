package io.github.sds100.keymapper.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.*
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.WidgetsManager.EVENT_SERVICE_START
import io.github.sds100.keymapper.WidgetsManager.EVENT_SERVICE_STOPPED
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.delegate.IKeymapDetectionDelegate
import io.github.sds100.keymapper.util.delegate.KeymapDetectionDelegate
import io.github.sds100.keymapper.util.isExternalCompat
import io.github.sds100.keymapper.util.show
import kotlinx.coroutines.delay

/**
 * Created by sds100 on 05/04/2020.
 */
class MyAccessibilityService : AccessibilityService(), LifecycleOwner, IKeymapDetectionDelegate {

    companion object {
        const val EXTRA_ACTION = "action"
        const val EXTRA_TIME_LEFT = "time"

        const val ACTION_RECORD_TRIGGER = "$PACKAGE_NAME.RECORD_TRIGGER"
        const val ACTION_TEST_ACTION = "$PACKAGE_NAME.TEST_ACTION"
        const val ACTION_STOP_RECORDING_TRIGGER = "$PACKAGE_NAME.STOP_RECORDING_TRIGGER"
        const val ACTION_RECORD_TRIGGER_TIMER_INCREMENTED = "$PACKAGE_NAME.TIMER_INCREMENTED"
        const val ACTION_RECORD_TRIGGER_KEY = "$PACKAGE_NAME.RECORD_TRIGGER_KEY"
        const val ACTION_PAUSE_REMAPPINGS = "$PACKAGE_NAME.PAUSE_REMAPPINGS"
        const val ACTION_RESUME_REMAPPINGS = "$PACKAGE_NAME.RESUME_REMAPPINGS"
        const val ACTION_UPDATE_NOTIFICATION = "$PACKAGE_NAME.UPDATE_NOTIFICATION"
        const val ACTION_START = "$PACKAGE_NAME.START_ACCESSIBILITY_SERVICE"
        const val ACTION_STOP = "$PACKAGE_NAME.STOP_ACCESSIBILITY_SERVICE"
        const val ACTION_ON_SERVICE_START = "$PACKAGE_NAME.ON_START_ACCESSIBILITY_SERVICE"
        const val ACTION_ON_SERVICE_STOP = "$PACKAGE_NAME.ON_STOP_ACCESSIBILITY_SERVICE"
        const val ACTION_SHOW_KEYBOARD = "$PACKAGE_NAME.SHOW_KEYBOARD"

        /**
         * How long should the accessibility service record a trigger in seconds.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5
    }

    /**
     * Broadcast receiver for all intents sent from within the app.
     */
    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_RECORD_TRIGGER -> {
                    //don't start recording if a trigger is being recorded
                    if (!mRecordingTrigger) {
                        mRecordingTrigger = true

                        lifecycleScope.launchWhenCreated {
                            recordTrigger()
                        }
                    }
                }

                ACTION_TEST_ACTION -> {
                    intent.getSerializableExtra(EXTRA_ACTION)?.let { action ->
                        //TODO test action
                    }
                }

                ACTION_PAUSE_REMAPPINGS -> {
                    mKeymapDetectionDelegate.reset()
                    mPaused = true
                    WidgetsManager.onEvent(this@MyAccessibilityService, WidgetsManager.EVENT_PAUSE_REMAPS)
                }

                ACTION_RESUME_REMAPPINGS -> {
                    mKeymapDetectionDelegate.reset()
                    mPaused = false
                    WidgetsManager.onEvent(this@MyAccessibilityService, WidgetsManager.EVENT_RESUME_REMAPS)
                }

                ACTION_UPDATE_NOTIFICATION -> {
                    if (mPaused) {
                        WidgetsManager.onEvent(this@MyAccessibilityService, WidgetsManager.EVENT_PAUSE_REMAPS)
                    } else {
                        WidgetsManager.onEvent(this@MyAccessibilityService, WidgetsManager.EVENT_RESUME_REMAPS)
                    }
                }

                Intent.ACTION_SCREEN_ON -> {
                    mKeymapDetectionDelegate.reset()
                }

                ACTION_SHOW_KEYBOARD ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        softKeyboardController.show(baseContext)
                    }
            }
        }
    }

    private var mRecordingTrigger = false

    private var mPaused = false
    private lateinit var mLifecycleRegistry: LifecycleRegistry

    private val mKeymapDetectionDelegate = KeymapDetectionDelegate(this)

    override fun onServiceConnected() {
        super.onServiceConnected()

        mLifecycleRegistry = LifecycleRegistry(this)
        mLifecycleRegistry.currentState = Lifecycle.State.STARTED

        IntentFilter().apply {
            addAction(ACTION_TEST_ACTION)
            addAction(ACTION_PAUSE_REMAPPINGS)
            addAction(ACTION_RESUME_REMAPPINGS)
            addAction(ACTION_UPDATE_NOTIFICATION)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(ACTION_RECORD_TRIGGER)
            addAction(ACTION_SHOW_KEYBOARD)

            registerReceiver(mBroadcastReceiver, this)
        }

        InjectorUtils.getKeymapRepository(this).keymapList.observe(this) {
            mKeymapDetectionDelegate.keyMapListCache = it
        }

        WidgetsManager.onEvent(this, EVENT_SERVICE_START)
        sendBroadcast(Intent(ACTION_ON_SERVICE_START))
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()

        mLifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        WidgetsManager.onEvent(this, EVENT_SERVICE_STOPPED)
        sendBroadcast(Intent(ACTION_ON_SERVICE_STOP))

        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return super.onKeyEvent(event)

        if (mRecordingTrigger) {
            if (event.action == KeyEvent.ACTION_DOWN) {

                //tell the UI that a key has been pressed

                Intent(ACTION_RECORD_TRIGGER_KEY).apply {
                    putExtra(Intent.EXTRA_KEY_EVENT, event)
                    sendBroadcast(this)
                }
            }

            return true
        }

        try {
            return mKeymapDetectionDelegate.onKeyEvent(
                event.keyCode,
                event.action,
                event.downTime,
                event.device.descriptor,
                event.device.isExternalCompat)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return super.onKeyEvent(event)
    }

    private suspend fun recordTrigger() {
        repeat(RECORD_TRIGGER_TIMER_LENGTH) { iteration ->
            val timeLeft = RECORD_TRIGGER_TIMER_LENGTH - iteration

            Intent(ACTION_RECORD_TRIGGER_TIMER_INCREMENTED).apply {
                putExtra(EXTRA_TIME_LEFT, timeLeft)

                sendBroadcast(this)
            }

            delay(1000)
        }

        sendBroadcast(Intent(ACTION_STOP_RECORDING_TRIGGER))

        mRecordingTrigger = false
    }

    override fun getLifecycle() = mLifecycleRegistry

    override fun performAction(action: Action) {
        Log.e(this::class.java.simpleName, "perform... ${action.uniqueId}")
    }

    override fun imitateButtonPress(keyCode: Int) {
        Log.e(this::class.java.simpleName, "imitate")
    }
}