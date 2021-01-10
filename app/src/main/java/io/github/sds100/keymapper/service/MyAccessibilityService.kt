package io.github.sds100.keymapper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.FingerprintGestureController
import android.bluetooth.BluetoothDevice
import android.content.*
import android.media.AudioManager
import android.os.Build.*
import android.os.SystemClock
import android.os.VibrationEffect
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.lifecycle.*
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.WidgetsManager.EVENT_ACCESSIBILITY_SERVICE_STARTED
import io.github.sds100.keymapper.WidgetsManager.EVENT_ACCESSIBILITY_SERVICE_STOPPED
import io.github.sds100.keymapper.WidgetsManager.EVENT_PAUSE_REMAPS
import io.github.sds100.keymapper.WidgetsManager.EVENT_RESUME_REMAPS
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.*
import io.github.sds100.keymapper.util.result.*
import kotlinx.coroutines.*
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag
import splitties.systemservices.displayManager
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
        const val ACTION_RECORD_TRIGGER = "$PACKAGE_NAME.RECORD_TRIGGER"
        const val ACTION_TEST_ACTION = "$PACKAGE_NAME.TEST_ACTION"
        const val ACTION_RECORDED_TRIGGER_KEY = "$PACKAGE_NAME.RECORDED_TRIGGER_KEY"
        const val ACTION_RECORD_TRIGGER_TIMER_INCREMENTED = "$PACKAGE_NAME.RECORD_TRIGGER_TIMER_INCREMENTED"
        const val ACTION_STOP_RECORDING_TRIGGER = "$PACKAGE_NAME.STOP_RECORDING_TRIGGER"
        const val ACTION_STOPPED_RECORDING_TRIGGER = "$PACKAGE_NAME.STOPPED_RECORDING_TRIGGER"
        const val ACTION_ON_START = "$PACKAGE_NAME.ON_ACCESSIBILITY_SERVICE_START"
        const val ACTION_ON_STOP = "$PACKAGE_NAME.ON_ACCESSIBILITY_SERVICE_STOP"
        const val ACTION_PERFORM_ACTIONS = "$PACKAGE_NAME.PERFORM_ACTIONS"
        const val ACTION_UPDATE_KEYMAP_LIST_CACHE = "$PACKAGE_NAME.UPDATE_KEYMAP_LIST_CACHE"

        //DONT CHANGE!!!
        const val ACTION_TRIGGER_KEYMAP_BY_UID = "$PACKAGE_NAME.TRIGGER_KEYMAP_BY_UID"
        const val EXTRA_KEYMAP_UID = "$PACKAGE_NAME.KEYMAP_UID"

        const val EXTRA_KEY_EVENT = "$PACKAGE_NAME.KEY_EVENT"
        const val EXTRA_TIME_LEFT = "$PACKAGE_NAME.TIME_LEFT"
        const val EXTRA_ACTION = "$PACKAGE_NAME.ACTION"
        const val EXTRA_ACTION_LIST = "$PACKAGE_NAME.ACTION_LIST"
        const val EXTRA_KEYMAP_LIST = "$PACKAGE_NAME.KEYMAP_LIST"

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
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return

                    if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                        mConnectedBtAddresses.remove(device.address)
                    } else {
                        mConnectedBtAddresses.add(device.address)
                    }
                }

                ACTION_SHOW_KEYBOARD -> {
                    if (VERSION.SDK_INT >= VERSION_CODES.N) {
                        softKeyboardController.show(baseContext)
                    }
                }

                ACTION_RECORD_TRIGGER -> {
                    //don't start recording if a trigger is being recorded
                    if (!mRecordingTrigger) {
                        mRecordingTriggerJob = recordTrigger()
                    }
                }

                ACTION_TEST_ACTION -> {
                    intent.getParcelableExtra<Action>(EXTRA_ACTION)?.let {
                        mActionPerformerDelegate.performAction(
                            it,
                            mChosenImePackageName,
                            currentPackageName
                        )
                    }
                }

                ACTION_STOP_RECORDING_TRIGGER -> {
                    val wasRecordingTrigger = mRecordingTrigger

                    mRecordingTriggerJob?.cancel()
                    mRecordingTriggerJob = null

                    if (wasRecordingTrigger) {
                        sendPackageBroadcast(ACTION_STOPPED_RECORDING_TRIGGER)
                    }
                }

                ACTION_PERFORM_ACTIONS -> {
                    val gson = GsonBuilder().registerTypeAdapter(Action.DESERIALIZER).create()
                    val actionListJson = intent.getStringExtra(EXTRA_ACTION_LIST) ?: return
                    val actionList = gson.fromJson<List<Action>>(actionListJson)

                    actionList.forEach {
                        mActionPerformerDelegate.performAction(
                            it,
                            mChosenImePackageName,
                            currentPackageName
                        )
                    }
                }

                ACTION_UPDATE_KEYMAP_LIST_CACHE -> {
                    intent.getStringExtra(EXTRA_KEYMAP_LIST)?.let {
                        val keymapList = Gson().fromJson<List<KeyMap>>(it)

                        updateKeymapListCache(keymapList)
                    }
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

                ACTION_TRIGGER_KEYMAP_BY_UID -> {
                    intent.getStringExtra(EXTRA_KEYMAP_UID)?.let {
                        mTriggerKeymapByIntentController.onDetected(it)
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
    private lateinit var mConstraintDelegate: ConstraintDelegate

    private lateinit var mTriggerKeymapByIntentController: TriggerKeymapByIntentController

    //fingerprint gesture stuff
    private lateinit var mFingerprintGestureMapController: FingerprintGestureMapController

    private var mFingerprintGestureCallback:
        FingerprintGestureController.FingerprintGestureCallback? = null

    private lateinit var mFingerprintMapRepository: FingerprintMapRepository

    override val currentTime: Long
        get() = SystemClock.elapsedRealtime()

    override val currentPackageName: String?
        get() = rootInActiveWindow?.packageName?.toString()

    override val isScreenOn: Boolean
        get() = mIsScreenOn

    override val orientation: Int?
        get() = displayManager.displays[0].rotation

    override val highestPriorityPackagePlayingMedia: String?
        get() = if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            MediaUtils.highestPriorityPackagePlayingMedia(this).valueOrNull()
        } else {
            null
        }

    private var mIsScreenOn = true
    private val mConnectedBtAddresses = mutableSetOf<String>()

    private var mChosenImePackageName: String? = null

    private val mIsCompatibleImeChosen
        get() = KeyboardUtils.KEY_MAPPER_IME_PACKAGE_LIST.contains(mChosenImePackageName)

    private val mGetEventDelegate = GetEventDelegate { keyCode, action, deviceDescriptor, isExternal, deviceId ->

        if (!AppPreferences.keymapsPaused) {
            withContext(Dispatchers.Main.immediate) {
                mKeymapDetectionDelegate.onKeyEvent(
                    keyCode,
                    action,
                    deviceDescriptor,
                    isExternal,
                    0,
                    deviceId
                )
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
            AppPreferences.holdDownDuration,
            AppPreferences.forceVibrate
        )

        mConstraintDelegate = ConstraintDelegate(this)

        mKeymapDetectionDelegate = KeymapDetectionDelegate(
            lifecycleScope,
            preferences,
            iClock = this,
            iActionError = this,
            mConstraintDelegate)

        mActionPerformerDelegate = ActionPerformerDelegate(
            context = this,
            iPerformAccessibilityAction = this,
            lifecycle = lifecycle)

        mTriggerKeymapByIntentController = TriggerKeymapByIntentController(
            coroutineScope = lifecycleScope,
            mConstraintDelegate,
            iActionError = this
        )

        IntentFilter().apply {
            addAction(ACTION_PAUSE_REMAPPINGS)
            addAction(ACTION_RESUME_REMAPPINGS)
            addAction(ACTION_SHOW_KEYBOARD)
            addAction(ACTION_RECORD_TRIGGER)
            addAction(ACTION_TEST_ACTION)
            addAction(ACTION_STOPPED_RECORDING_TRIGGER)
            addAction(ACTION_PERFORM_ACTIONS)
            addAction(ACTION_UPDATE_KEYMAP_LIST_CACHE)
            addAction(ACTION_STOP_RECORDING_TRIGGER)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(ACTION_TRIGGER_KEYMAP_BY_UID)

            registerReceiver(mBroadcastReceiver, this)
        }

        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)

        WidgetsManager.onEvent(this, EVENT_ACCESSIBILITY_SERVICE_STARTED)
        sendPackageBroadcast(ACTION_ON_START)

        mKeymapDetectionDelegate.imitateButtonPress.observe(this, {
            when (it.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> AudioUtils.adjustVolume(this, AudioManager.ADJUST_RAISE,
                    showVolumeUi = true)

                KeyEvent.KEYCODE_VOLUME_DOWN -> AudioUtils.adjustVolume(this, AudioManager.ADJUST_LOWER,
                    showVolumeUi = true)

                KeyEvent.KEYCODE_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                KeyEvent.KEYCODE_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
                KeyEvent.KEYCODE_APP_SWITCH -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                KeyEvent.KEYCODE_MENU ->
                    mActionPerformerDelegate.performSystemAction(
                        SystemAction.OPEN_MENU,
                        mChosenImePackageName,
                        currentPackageName
                    )

                else -> {
                    mChosenImePackageName?.let { imePackageName ->
                        KeyboardUtils.inputKeyEventFromImeService(
                            imePackageName = imePackageName,
                            keyCode = it.keyCode,
                            metaState = it.metaState,
                            deviceId = it.deviceId
                        )
                    }
                }
            }
        })

        mChosenImePackageName = KeyboardUtils.getChosenInputMethodPackageName(this).valueOrNull()

        mFingerprintMapRepository = ServiceLocator.fingerprintMapRepository(this)

        mFingerprintGestureMapController = FingerprintGestureMapController(
            lifecycleScope,
            iConstraintDelegate = mConstraintDelegate,
            iActionError = this
        )

        if (VERSION.SDK_INT >= VERSION_CODES.O) {

            requestFingerprintGestureDetection()

            lifecycleScope.launchWhenStarted {
                mFingerprintMapRepository.setFingerprintGesturesAvailable(
                    fingerprintGestureController.isGestureDetectionAvailable)
            }

            mFingerprintGestureCallback =
                object : FingerprintGestureController.FingerprintGestureCallback() {

                    override fun onGestureDetected(gesture: Int) {
                        super.onGestureDetected(gesture)

                        mFingerprintGestureMapController.onGesture(gesture)
                    }
                }

            observeFingerprintMaps(mFingerprintMapRepository)

            mFingerprintGestureCallback?.let {
                fingerprintGestureController.registerFingerprintGestureCallback(it, null)
            }
        }

        lifecycleScope.launchWhenStarted {
            val keymapList = withContext(Dispatchers.IO) {
                ServiceLocator.keymapRepository(this@MyAccessibilityService).getKeymaps()
            }

            withContext(Dispatchers.Main) {
                updateKeymapListCache(keymapList)
            }
        }

        MediatorLiveData<Vibrate>().apply {
            addSource(mKeymapDetectionDelegate.vibrate) {
                value = it
            }

            addSource(mFingerprintGestureMapController.vibrate) {
                value = it
            }

            addSource(mTriggerKeymapByIntentController.vibrate) {
                value = it
            }

            observe(this@MyAccessibilityService, Observer {
                if (it.duration <= 0) return@Observer

                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    val effect =
                        VibrationEffect.createOneShot(it.duration, VibrationEffect.DEFAULT_AMPLITUDE)

                    vibrator.vibrate(effect)
                } else {
                    vibrator.vibrate(it.duration)
                }
            })
        }

        MediatorLiveData<PerformAction>().apply {
            addSource(mKeymapDetectionDelegate.performAction) {
                value = it
            }

            addSource(mFingerprintGestureMapController.performAction) {
                value = it
            }

            addSource(mTriggerKeymapByIntentController.performAction) {
                value = it
            }

            observe(this@MyAccessibilityService, Observer {
                mActionPerformerDelegate.performAction(
                    it,
                    mChosenImePackageName,
                    currentPackageName
                )
            })
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {

        if (::mLifecycleRegistry.isInitialized) {
            mLifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        WidgetsManager.onEvent(this, EVENT_ACCESSIBILITY_SERVICE_STOPPED)

        sendPackageBroadcast(ACTION_ON_STOP)

        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        unregisterReceiver(mBroadcastReceiver)

        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            fingerprintGestureController
                .unregisterFingerprintGestureCallback(mFingerprintGestureCallback)

            mFingerprintGestureMapController.reset()
        }

        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return super.onKeyEvent(event)

        if (mRecordingTrigger) {
            if (event.action == KeyEvent.ACTION_DOWN) {

                //tell the UI that a key has been pressed
                sendPackageBroadcast(ACTION_RECORDED_TRIGGER_KEY, bundleOf(EXTRA_KEY_EVENT to event))
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
                    event.metaState,
                    event.deviceId)
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

            str(R.string.key_pref_hold_down_duration) -> {
                mKeymapDetectionDelegate.preferences.defaultHoldDownDuration = AppPreferences.holdDownDuration
            }

            str(R.string.key_pref_keymaps_paused) -> {
                if (AppPreferences.keymapsPaused) {
                    WidgetsManager.onEvent(this, EVENT_PAUSE_REMAPS)

                    if (AppPreferences.toggleKeyboardOnToggleKeymaps) {
                        KeyboardUtils.chooseLastUsedIncompatibleInputMethod(this)
                    }

                    if (VERSION.SDK_INT >= VERSION_CODES.O) {
                        denyFingerprintGestureDetection()
                    }
                } else {
                    WidgetsManager.onEvent(this, EVENT_RESUME_REMAPS)

                    if (AppPreferences.toggleKeyboardOnToggleKeymaps) {
                        KeyboardUtils.chooseCompatibleInputMethod(this)
                    }

                    if (VERSION.SDK_INT >= VERSION_CODES.O) {
                        requestFingerprintGestureDetection()
                    }
                }
            }
        }
    }

    override fun isBluetoothDeviceConnected(address: String) = mConnectedBtAddresses.contains(address)

    override fun canActionBePerformed(action: Action): Result<Action> {
        if (action.requiresIME) {
            return if (mIsCompatibleImeChosen) {
                Success(action)
            } else {
                NoCompatibleImeChosen()
            }
        }

        return action.canBePerformed(this)
    }

    override fun getLifecycle() = mLifecycleRegistry

    override val keyboardController: SoftKeyboardController?
        get() = if (VERSION.SDK_INT >= VERSION_CODES.N) {
            softKeyboardController
        } else {
            null
        }

    override val rootNode: AccessibilityNodeInfo?
        get() = rootInActiveWindow

    @RequiresApi(VERSION_CODES.O)
    private fun requestFingerprintGestureDetection() {
        serviceInfo = serviceInfo.apply {
            flags = flags.withFlag(AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES)
        }
    }

    @RequiresApi(VERSION_CODES.O)
    private fun denyFingerprintGestureDetection() {
        serviceInfo = serviceInfo?.apply {
            flags = flags.minusFlag(AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES)
        }
    }

    @RequiresApi(VERSION_CODES.O)
    private fun observeFingerprintMaps(repository: FingerprintMapRepository) {

        repository.fingerprintGestureMapsLiveData.observe(this, Observer { maps ->
            mFingerprintGestureMapController.fingerprintMaps = maps

            invalidateFingerprintGestureDetection()
        })
    }

    @RequiresApi(VERSION_CODES.O)
    private fun invalidateFingerprintGestureDetection() {
        mFingerprintGestureMapController.fingerprintMaps.let { maps ->
            if (maps.any { it.value.isEnabled && it.value.actionList.isNotEmpty() }
                && !AppPreferences.keymapsPaused) {
                requestFingerprintGestureDetection()
            } else {
                denyFingerprintGestureDetection()
            }
        }
    }

    private fun recordTrigger() = lifecycleScope.launchWhenStarted {
        repeat(RECORD_TRIGGER_TIMER_LENGTH) { iteration ->
            if (isActive) {
                val timeLeft = RECORD_TRIGGER_TIMER_LENGTH - iteration

                sendPackageBroadcast(
                    ACTION_RECORD_TRIGGER_TIMER_INCREMENTED,
                    bundleOf(EXTRA_TIME_LEFT to timeLeft)
                )

                delay(1000)
            }
        }

        sendPackageBroadcast(ACTION_STOP_RECORDING_TRIGGER)
    }

    private fun updateKeymapListCache(keymapList: List<KeyMap>) {
        mKeymapDetectionDelegate.keyMapListCache = keymapList

        mScreenOffTriggersEnabled = keymapList.any { keymap ->
            keymap.trigger.flags.hasFlag(Trigger.TRIGGER_FLAG_SCREEN_OFF_TRIGGERS)
        }

        mTriggerKeymapByIntentController.onKeymapListUpdate(keymapList)
    }
}