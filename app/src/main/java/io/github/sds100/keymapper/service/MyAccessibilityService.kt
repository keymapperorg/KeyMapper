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
import androidx.core.os.bundleOf
import androidx.lifecycle.*
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.NotificationController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.data.*
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.globalPreferences
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.*
import io.github.sds100.keymapper.util.result.*
import kotlinx.coroutines.*
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
    IClock,
    IAccessibilityService,
    IConstraintState,
    IActionError {

    companion object {

        const val ACTION_START_SERVICE = "$PACKAGE_NAME.START_ACCESSIBILITY_SERVICE"
        const val ACTION_STOP_SERVICE = "$PACKAGE_NAME.STOP_ACCESSIBILITY_SERVICE"
        const val ACTION_SHOW_KEYBOARD = "$PACKAGE_NAME.SHOW_KEYBOARD"
        const val ACTION_RECORD_TRIGGER = "$PACKAGE_NAME.RECORD_TRIGGER"
        const val ACTION_TEST_ACTION = "$PACKAGE_NAME.TEST_ACTION"
        const val ACTION_RECORDED_TRIGGER_KEY = "$PACKAGE_NAME.RECORDED_TRIGGER_KEY"
        const val ACTION_RECORD_TRIGGER_TIMER_INCREMENTED = "$PACKAGE_NAME.RECORD_TRIGGER_TIMER_INCREMENTED"
        const val ACTION_STOP_RECORDING_TRIGGER = "$PACKAGE_NAME.STOP_RECORDING_TRIGGER"
        const val ACTION_STOPPED_RECORDING_TRIGGER = "$PACKAGE_NAME.STOPPED_RECORDING_TRIGGER"
        const val ACTION_ON_START = "$PACKAGE_NAME.ON_ACCESSIBILITY_SERVICE_START"
        const val ACTION_ON_STOP = "$PACKAGE_NAME.ON_ACCESSIBILITY_SERVICE_STOP"
        const val ACTION_UPDATE_KEYMAP_LIST_CACHE = "$PACKAGE_NAME.UPDATE_KEYMAP_LIST_CACHE"

        //DONT CHANGE!!!
        const val ACTION_TRIGGER_KEYMAP_BY_UID = "$PACKAGE_NAME.TRIGGER_KEYMAP_BY_UID"
        const val EXTRA_KEYMAP_UID = "$PACKAGE_NAME.KEYMAP_UID"

        const val EXTRA_RECORDED_TRIGGER_KEY_EVENT = "$PACKAGE_NAME.EXTRA_RECORDED_TRIGGER_KEY_EVENT"

        const val EXTRA_TIME_LEFT = "$PACKAGE_NAME.EXTRA_TIME_LEFT"
        const val EXTRA_ACTION = "$PACKAGE_NAME.EXTRA_ACTION"
        const val EXTRA_KEYMAP_LIST = "$PACKAGE_NAME.EXTRA_KEYMAP_LIST"
    }

    /**
     * Broadcast receiver for all intents sent from within the app.
     */
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {

                BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return

                    if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                        connectedBtAddresses.remove(device.address)
                    } else {
                        connectedBtAddresses.add(device.address)
                    }
                }

                ACTION_SHOW_KEYBOARD -> {
                    if (VERSION.SDK_INT >= VERSION_CODES.N) {
                        softKeyboardController.show(this@MyAccessibilityService)
                    }
                }

                ACTION_RECORD_TRIGGER -> controller.startRecordingTrigger()

                ACTION_TEST_ACTION -> {
                    intent.getParcelableExtra<Action>(EXTRA_ACTION)?.let {
                        actionPerformerDelegate.performAction(
                            it,
                            chosenImePackageName,
                            currentPackageName
                        )
                    }
                }

                ACTION_STOP_RECORDING_TRIGGER -> controller.stopRecordingTrigger()

                ACTION_UPDATE_KEYMAP_LIST_CACHE -> {
                    intent.getStringExtra(EXTRA_KEYMAP_LIST)?.let {
                        val keymapList = Gson().fromJson<List<KeyMap>>(it)

                        controller.updateKeymapListCache(keymapList)
                    }
                }

                Intent.ACTION_SCREEN_ON -> {
                    _isScreenOn = true
                    controller.onScreenOn()
                }

                Intent.ACTION_SCREEN_OFF -> {
                    _isScreenOn = false
                    controller.onScreenOff()
                }

                ACTION_TRIGGER_KEYMAP_BY_UID -> {
                    intent.getStringExtra(EXTRA_KEYMAP_UID)?.let {
                        controller.triggerKeymapByIntent(it)
                    }
                }

                Intent.ACTION_INPUT_METHOD_CHANGED -> {
                    chosenImePackageName =
                        KeyboardUtils.getChosenInputMethodPackageName(this@MyAccessibilityService)
                            .valueOrNull()
                }
            }
        }
    }

    private lateinit var lifecycleRegistry: LifecycleRegistry

    private lateinit var actionPerformerDelegate: ActionPerformerDelegate

    private var fingerprintGestureCallback:
        FingerprintGestureController.FingerprintGestureCallback? = null

    override val currentTime: Long
        get() = SystemClock.elapsedRealtime()

    override val currentPackageName: String?
        get() = rootInActiveWindow?.packageName?.toString()

    private var _isScreenOn = true
    override val isScreenOn: Boolean
        get() = _isScreenOn

    override val orientation: Int?
        get() = displayManager.displays[0].rotation

    override val highestPriorityPackagePlayingMedia: String?
        get() = if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            MediaUtils.highestPriorityPackagePlayingMedia(this).valueOrNull()
        } else {
            null
        }

    override val isGestureDetectionAvailable: Boolean
        get() = if (VERSION.SDK_INT >= VERSION_CODES.O) {
            fingerprintGestureController.isGestureDetectionAvailable
        } else {
            false
        }

    private val connectedBtAddresses = mutableSetOf<String>()

    private var chosenImePackageName: String? = null

    private val isCompatibleImeChosen
        get() = KeyboardUtils.KEY_MAPPER_IME_PACKAGE_LIST.contains(chosenImePackageName)

    private val notificationController: NotificationController
        get() = ServiceLocator.notificationController(this)

    private lateinit var controller: AccessibilityServiceController

    override fun onServiceConnected() {
        super.onServiceConnected()

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        actionPerformerDelegate = ActionPerformerDelegate(
            context = this,
            iAccessibilityService = this,
            lifecycle = lifecycle)

        IntentFilter().apply {
            addAction(ACTION_SHOW_KEYBOARD)
            addAction(ACTION_RECORD_TRIGGER)
            addAction(ACTION_TEST_ACTION)
            addAction(ACTION_STOPPED_RECORDING_TRIGGER)
            addAction(ACTION_UPDATE_KEYMAP_LIST_CACHE)
            addAction(ACTION_STOP_RECORDING_TRIGGER)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(ACTION_TRIGGER_KEYMAP_BY_UID)
            addAction(Intent.ACTION_INPUT_METHOD_CHANGED)

            registerReceiver(broadcastReceiver, this)
        }

        notificationController.onEvent(OnAccessibilityServiceStarted)
        sendPackageBroadcast(ACTION_ON_START)

        chosenImePackageName = KeyboardUtils.getChosenInputMethodPackageName(this).valueOrNull()

        if (VERSION.SDK_INT >= VERSION_CODES.O) {

            fingerprintGestureCallback =
                object : FingerprintGestureController.FingerprintGestureCallback() {

                    override fun onGestureDetected(gesture: Int) {
                        super.onGestureDetected(gesture)

                        controller.onFingerprintGesture(gesture)
                    }
                }

            fingerprintGestureCallback?.let {
                fingerprintGestureController.registerFingerprintGestureCallback(it, null)
            }
        }

        controller = AccessibilityServiceController(
            lifecycleOwner = this,
            constraintState = this,
            iAccessibilityService = this,
            clock = this,
            actionError = this,
            appUpdateManager = ServiceLocator.appUpdateManager(this),
            globalPreferences = ServiceLocator.globalPreferences(this),
            fingerprintMapRepository = ServiceLocator.fingerprintMapRepository(this),
            keymapRepository = ServiceLocator.keymapRepository(this)
        )

        controller.eventStream.observe(this, Observer {
            onControllerEvent(it)
        })

        globalPreferences.keymapsPaused.collectWhenStarted(this) { paused ->
            Timber.e("paused $paused")
            if (paused) {
                globalPreferences.getFlow(Keys.toggleKeyboardOnToggleKeymaps)
                    .firstBlocking()
                    .let {
                        if (it == true) {
                            KeyboardUtils.chooseLastUsedIncompatibleInputMethod(this)
                        }
                    }
            } else {
                globalPreferences.getFlow(Keys.toggleKeyboardOnToggleKeymaps)
                    .firstBlocking()
                    .let {
                        if (it == true) {
                            KeyboardUtils.chooseCompatibleInputMethod(this)
                        }
                    }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {

        if (::lifecycleRegistry.isInitialized) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        notificationController.onEvent(OnAccessibilityServiceStopped)

        sendPackageBroadcast(ACTION_ON_STOP)

        unregisterReceiver(broadcastReceiver)

        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            fingerprintGestureController
                .unregisterFingerprintGestureCallback(fingerprintGestureCallback)
        }

        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return super.onKeyEvent(event)

        return controller.onKeyEvent(event.keyCode,
            event.action,
            event.device.name,
            event.device.descriptor,
            event.device.isExternalCompat,
            event.metaState,
            event.deviceId,
            event.scanCode)
    }

    override fun isBluetoothDeviceConnected(address: String) = connectedBtAddresses.contains(address)

    override fun canActionBePerformed(action: Action): Result<Action> {
        if (action.requiresIME) {
            return if (isCompatibleImeChosen) {
                Success(action)
            } else {
                NoCompatibleImeChosen()
            }
        }

        return action.canBePerformed(this)
    }

    override fun getLifecycle() = lifecycleRegistry

    override val keyboardController: SoftKeyboardController?
        get() = if (VERSION.SDK_INT >= VERSION_CODES.N) {
            softKeyboardController
        } else {
            null
        }

    override val rootNode: AccessibilityNodeInfo?
        get() = rootInActiveWindow

    override fun requestFingerprintGestureDetection() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            serviceInfo = serviceInfo.apply {
                flags = flags.withFlag(AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES)
            }
        }
    }

    override fun denyFingerprintGestureDetection() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            serviceInfo = serviceInfo?.apply {
                flags = flags.minusFlag(AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES)
            }
        }
    }

    private fun onControllerEvent(event: Event) {
        when (event) {
            is OnIncrementRecordTriggerTimer -> sendPackageBroadcast(
                ACTION_RECORD_TRIGGER_TIMER_INCREMENTED,
                bundleOf(EXTRA_TIME_LEFT to event.timeLeft)
            )

            is ShowFingerprintFeatureNotification ->
                notificationController.onEvent(ShowFingerprintFeatureNotification)

            is OnStoppedRecordingTrigger ->
                sendPackageBroadcast(ACTION_STOPPED_RECORDING_TRIGGER)

            is VibrateEvent -> {
                if (event.duration <= 0) return

                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    val effect =
                        VibrationEffect.createOneShot(event.duration, VibrationEffect.DEFAULT_AMPLITUDE)

                    vibrator.vibrate(effect)
                } else {
                    vibrator.vibrate(event.duration)
                }
            }

            is PerformAction -> actionPerformerDelegate.performAction(
                event,
                chosenImePackageName,
                currentPackageName
            )

            is ShowTriggeredKeymapToast -> toast(R.string.toast_triggered_keymap)

            is RecordedTriggerKeyEvent ->  //tell the UI that a key has been pressed
                sendPackageBroadcast(ACTION_RECORDED_TRIGGER_KEY,
                    bundleOf(EXTRA_RECORDED_TRIGGER_KEY_EVENT to event))

            is ImitateButtonPress -> when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP ->
                    AudioUtils.adjustVolume(this, AudioManager.ADJUST_RAISE, showVolumeUi = true)

                KeyEvent.KEYCODE_VOLUME_DOWN ->
                    AudioUtils.adjustVolume(this, AudioManager.ADJUST_LOWER, showVolumeUi = true)

                KeyEvent.KEYCODE_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                KeyEvent.KEYCODE_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
                KeyEvent.KEYCODE_APP_SWITCH -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                KeyEvent.KEYCODE_MENU ->
                    actionPerformerDelegate.performSystemAction(
                        SystemAction.OPEN_MENU,
                        chosenImePackageName,
                        currentPackageName
                    )

                else -> {
                    chosenImePackageName?.let { imePackageName ->
                        KeyboardUtils.inputKeyEventFromImeService(
                            this,
                            imePackageName = imePackageName,
                            keyCode = event.keyCode,
                            metaState = event.metaState,
                            keyEventAction = event.keyEventAction,
                            deviceId = event.deviceId,
                            scanCode = event.scanCode
                        )
                    }
                }
            }
        }
    }
}