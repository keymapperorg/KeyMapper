package io.github.sds100.keymapper.service

import android.accessibilityservice.AccessibilityService
import android.bluetooth.BluetoothDevice
import android.content.*
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.MainThread
import androidx.lifecycle.*
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.MyApplication
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.WidgetsManager.EVENT_ACCESSIBILITY_SERVICE_START
import io.github.sds100.keymapper.WidgetsManager.EVENT_ACCESSIBILITY_SERVICE_STOPPED
import io.github.sds100.keymapper.WidgetsManager.EVENT_PAUSE_REMAPS
import io.github.sds100.keymapper.WidgetsManager.EVENT_RESUME_REMAPS
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.ActionPerformerDelegate
import io.github.sds100.keymapper.util.delegate.GetEventDelegate
import io.github.sds100.keymapper.util.delegate.KeymapDetectionDelegate
import io.github.sds100.keymapper.util.delegate.KeymapDetectionPreferences
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.SelectedCompatibleImeNotChosen
import io.github.sds100.keymapper.util.result.Success
import kotlinx.coroutines.*
import splitties.bitflags.hasFlag
import splitties.systemservices.vibrator
import splitties.toast.toast
import timber.log.Timber

/**
 * Created by sds100 on 05/04/2020.
 */
class MyAccessibilityService : AccessibilityService(),
    LifecycleOwner,
    SharedPreferences.OnSharedPreferenceChangeListener,
    IClock,
    IPerformAccessibilityAction,
    IConstraintState,
    IActionError {

    companion object {

        const val ACTION_PAUSE_REMAPPINGS = "$PACKAGE_NAME.PAUSE_REMAPPINGS"
        const val ACTION_RESUME_REMAPPINGS = "$PACKAGE_NAME.RESUME_REMAPPINGS"
        const val ACTION_START = "$PACKAGE_NAME.START_ACCESSIBILITY_SERVICE"
        const val ACTION_STOP = "$PACKAGE_NAME.STOP_ACCESSIBILITY_SERVICE"
        const val ACTION_SHOW_KEYBOARD = "$PACKAGE_NAME.SHOW_KEYBOARD"

        const val EVENT_RECORD_TRIGGER = "record_trigger"
        const val EVENT_RECORD_TRIGGER_KEY = "record_trigger_key"
        const val EVENT_RECORD_TRIGGER_TIMER_INCREMENTED = "record_trigger_timer_incremented"
        const val EVENT_STOP_RECORDING_TRIGGER = "stop_recording_trigger"
        const val EVENT_TEST_ACTION = "test_action"
        const val EVENT_ON_SERVICE_STOPPED = "accessibility_service_stopped"
        const val EVENT_ON_SERVICE_STARTED = "accessibility_service_started"

        private lateinit var BUS: MutableLiveData<Event<Pair<String, Any?>>>

        @MainThread
        fun provideBus(): MutableLiveData<Event<Pair<String, Any?>>> {
            BUS = if (::BUS.isInitialized) BUS else MutableLiveData()

            return BUS
        }

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

                ACTION_PAUSE_REMAPPINGS -> {
                    mKeymapDetectionDelegate.reset()
                    AppPreferences.keymapsPaused = true
                    WidgetsManager.onEvent(this@MyAccessibilityService, EVENT_PAUSE_REMAPS)
                }

                ACTION_RESUME_REMAPPINGS -> {
                    mKeymapDetectionDelegate.reset()
                    AppPreferences.keymapsPaused = false
                    WidgetsManager.onEvent(this@MyAccessibilityService, EVENT_RESUME_REMAPS)
                }

                BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return

                    if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                        mConnectedBtAddresses.remove(device.address)
                    } else {
                        mConnectedBtAddresses.add(device.address)
                    }
                }

                ACTION_SHOW_KEYBOARD -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        softKeyboardController.show(baseContext)
                    }
                }

                Intent.ACTION_INPUT_METHOD_CHANGED -> {
                    mIsCompatibleImeChosen = KeyboardUtils.isSelectedImeChosen()
                }

                Intent.ACTION_SCREEN_ON -> {
                    mIsScreenOn = true
                    mGetEventDelegate.stopListening()
                }

                Intent.ACTION_SCREEN_OFF -> {
                    mIsScreenOn = false
                    if (AppPreferences.hasRootPermission && mScreenOffTriggersEnabled) {
                        if (!mGetEventDelegate.startListening(lifecycleScope)) {
                            toast(R.string.error_failed_execute_getevent)
                        }
                    }
                }
            }
        }
    }

    private var mRecordingTriggerJob: Job? = null

    private val mRecordingTrigger: Boolean
        get() = mRecordingTriggerJob != null

    private var mScreenOffTriggersEnabled = false

    private lateinit var mLifecycleRegistry: LifecycleRegistry

    private lateinit var mKeymapDetectionDelegate: KeymapDetectionDelegate
    private lateinit var mActionPerformerDelegate: ActionPerformerDelegate

    override val currentTime: Long
        get() = SystemClock.elapsedRealtime()

    override val currentPackageName: String
        get() = rootInActiveWindow.packageName.toString()

    override val isScreenOn: Boolean
        get() = mIsScreenOn

    private var mIsScreenOn = true
    private val mConnectedBtAddresses = mutableSetOf<String>()
    private var mIsCompatibleImeChosen = false

    private val mGetEventDelegate = GetEventDelegate { keyCode, action, deviceDescriptor, isExternal ->
        if (!AppPreferences.keymapsPaused) {
            withContext(Dispatchers.Main.immediate) {
                mKeymapDetectionDelegate.onKeyEvent(keyCode, action, deviceDescriptor, isExternal, 0)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        mLifecycleRegistry = LifecycleRegistry(this)
        mLifecycleRegistry.currentState = Lifecycle.State.STARTED

        val preferences = KeymapDetectionPreferences(
            AppPreferences.longPressDelay,
            AppPreferences.doublePressDelay,
            AppPreferences.repeatDelay,
            AppPreferences.repeatRate,
            AppPreferences.sequenceTriggerTimeout,
            AppPreferences.vibrateDuration,
            AppPreferences.forceVibrate
        )

        mKeymapDetectionDelegate = KeymapDetectionDelegate(
            lifecycleScope,
            preferences,
            iClock = this,
            iConstraintState = this,
            iActionError = this)

        mActionPerformerDelegate = ActionPerformerDelegate(
            context = this,
            iPerformAccessibilityAction = this,
            lifecycle = lifecycle)

        IntentFilter().apply {
            addAction(ACTION_PAUSE_REMAPPINGS)
            addAction(ACTION_RESUME_REMAPPINGS)
            addAction(ACTION_SHOW_KEYBOARD)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(Intent.ACTION_INPUT_METHOD_CHANGED)

            registerReceiver(mBroadcastReceiver, this)
        }

        (application as MyApplication).keymapRepository.keymapList.observe(this) {
            mKeymapDetectionDelegate.keyMapListCache = it
            mScreenOffTriggersEnabled = it.any { keymap ->
                keymap.trigger.flags.hasFlag(Trigger.TRIGGER_FLAG_SCREEN_OFF_TRIGGERS)
            }
        }

        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)

        WidgetsManager.onEvent(this, EVENT_ACCESSIBILITY_SERVICE_START)
        provideBus().value = Event(EVENT_ON_SERVICE_STARTED to null)

        mKeymapDetectionDelegate.imitateButtonPress.observe(this, EventObserver {
            when (it.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> AudioUtils.adjustVolume(this, AudioManager.ADJUST_RAISE,
                    showVolumeUi = true)

                KeyEvent.KEYCODE_VOLUME_DOWN -> AudioUtils.adjustVolume(this, AudioManager.ADJUST_LOWER,
                    showVolumeUi = true)

                KeyEvent.KEYCODE_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                KeyEvent.KEYCODE_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
                KeyEvent.KEYCODE_APP_SWITCH -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                KeyEvent.KEYCODE_MENU -> mActionPerformerDelegate.performSystemAction(SystemAction.OPEN_MENU)

                else -> KeyboardUtils.inputKeyEventFromImeService(
                    keyCode = it.keyCode,
                    metaState = it.metaState
                )
            }
        })

        mKeymapDetectionDelegate.performAction.observe(this, EventObserver { model ->
            mActionPerformerDelegate.performAction(model)
        })

        mKeymapDetectionDelegate.vibrate.observe(this, EventObserver {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(it, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(it)
            }
        })

        provideBus().observe(this, Observer {
            it ?: return@Observer

            when (it.peekContent().first) {
                EVENT_RECORD_TRIGGER -> {
                    //don't start recording if a trigger is being recorded
                    if (!mRecordingTrigger) {
                        mRecordingTriggerJob = recordTrigger()
                    }

                    it.handled()
                }

                EVENT_TEST_ACTION -> {
                    (it.getContentIfNotHandled()?.second as Action?)?.let { action ->
                        mActionPerformerDelegate.performAction(action)
                    }
                }

                EVENT_STOP_RECORDING_TRIGGER -> {
                    mRecordingTriggerJob?.cancel()
                    mRecordingTriggerJob = null
                }

                Intent.ACTION_SCREEN_ON -> {
                    mKeymapDetectionDelegate.reset()
                    it.handled()
                }
            }
        })

        mIsCompatibleImeChosen = KeyboardUtils.isSelectedImeChosen()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()

        if (::mLifecycleRegistry.isInitialized) {
            mLifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        WidgetsManager.onEvent(this, EVENT_ACCESSIBILITY_SERVICE_STOPPED)
        provideBus().value = Event(EVENT_ON_SERVICE_STOPPED to null)
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return super.onKeyEvent(event)

        if (mRecordingTrigger) {
            if (event.action == KeyEvent.ACTION_DOWN) {

                //tell the UI that a key has been pressed
                provideBus().value = Event(EVENT_RECORD_TRIGGER_KEY to event)
            }

            return true
        }

        if (!AppPreferences.keymapsPaused) {
            try {
                return mKeymapDetectionDelegate.onKeyEvent(
                    event.keyCode,
                    event.action,
                    event.device.descriptor,
                    event.device.isExternalCompat,
                    event.metaState)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        return super.onKeyEvent(event)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            str(R.string.key_pref_long_press_delay) -> {
                mKeymapDetectionDelegate.preferences.defaultLongPressDelay = AppPreferences.longPressDelay
            }

            str(R.string.key_pref_double_press_delay) -> {
                mKeymapDetectionDelegate.preferences.defaultDoublePressDelay = AppPreferences.doublePressDelay
            }

            str(R.string.key_pref_repeat_delay) -> {
                mKeymapDetectionDelegate.preferences.defaultRepeatDelay = AppPreferences.repeatDelay
            }

            str(R.string.key_pref_repeat_rate) -> {
                mKeymapDetectionDelegate.preferences.defaultRepeatRate = AppPreferences.repeatRate
            }

            str(R.string.key_pref_sequence_trigger_timeout) -> {
                mKeymapDetectionDelegate.preferences.defaultSequenceTriggerTimeout =
                    AppPreferences.sequenceTriggerTimeout
            }

            str(R.string.key_pref_vibrate_duration) -> {
                mKeymapDetectionDelegate.preferences.defaultVibrateDuration = AppPreferences.vibrateDuration
            }

            str(R.string.key_pref_force_vibrate) -> {
                mKeymapDetectionDelegate.preferences.forceVibrate = AppPreferences.forceVibrate
            }

            str(R.string.key_pref_keymaps_paused) -> {
                if (AppPreferences.keymapsPaused) {
                    WidgetsManager.onEvent(this, EVENT_PAUSE_REMAPS)
                } else {
                    WidgetsManager.onEvent(this, EVENT_RESUME_REMAPS)
                }
            }

            str(R.string.key_pref_selected_compatible_ime_package_name) -> {
                mIsCompatibleImeChosen = KeyboardUtils.isSelectedImeChosen()
            }
        }
    }

    override fun isBluetoothDeviceConnected(address: String) = mConnectedBtAddresses.contains(address)

    override fun canActionBePerformed(action: Action): Result<Action> {
        if (action.requiresIME) {
            return if (mIsCompatibleImeChosen) {
                Success(action)
            } else {
                SelectedCompatibleImeNotChosen()
            }
        }

        return action.canBePerformed(this)
    }

    private fun recordTrigger() = lifecycleScope.launch {
        repeat(RECORD_TRIGGER_TIMER_LENGTH) { iteration ->
            if (isActive) {
                val timeLeft = RECORD_TRIGGER_TIMER_LENGTH - iteration

                provideBus().value = Event(EVENT_RECORD_TRIGGER_TIMER_INCREMENTED to timeLeft)

                delay(1000)
            }
        }

        provideBus().value = Event(EVENT_STOP_RECORDING_TRIGGER to null)
    }

    override fun getLifecycle() = mLifecycleRegistry

    override val keyboardController: SoftKeyboardController?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            softKeyboardController
        } else {
            null
        }

    override val rootNode: AccessibilityNodeInfo?
        get() = rootInActiveWindow
}