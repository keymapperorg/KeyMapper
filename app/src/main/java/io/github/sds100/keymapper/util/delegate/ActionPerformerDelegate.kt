package io.github.sds100.keymapper.util.delegate

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.DEVICE_POLICY_SERVICE
import android.content.Intent
import android.graphics.Path
import android.hardware.camera2.CameraCharacteristics
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.InputEvent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.URLUtil
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.lifecycle.Lifecycle
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.TapGesture
import io.github.sds100.keymapper.data.hasRootPermission
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Option
import io.github.sds100.keymapper.data.model.getData
import io.github.sds100.keymapper.globalPreferences
import io.github.sds100.keymapper.service.TapperAccessibilityService
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag
import splitties.toast.longToast
import splitties.toast.toast


/**
 * Created by sds100 on 25/11/2018.
 */

class ActionPerformerDelegate(context: Context,
                              iAccessibilityService: IAccessibilityService,
                              lifecycle: Lifecycle
) : IAccessibilityService by iAccessibilityService {

    companion object {
        private const val OVERFLOW_MENU_CONTENT_DESCRIPTION = "More options"
    }

    private val ctx = context.applicationContext
    private lateinit var flashlightController: FlashlightController
    private val suProcessDelegate = SuProcessDelegate()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flashlightController = FlashlightController()
            lifecycle.addObserver(flashlightController)
        }

        lifecycle.addObserver(suProcessDelegate)
    }

    fun performAction(
            action: Action,
            chosenImePackageName: String?,
            currentPackageName: String?
    ) = performAction(PerformAction(action), chosenImePackageName, currentPackageName)

    fun performAction(
            performActionModel: PerformAction,
            chosenImePackageName: String?,
            currentPackageName: String?,

            ) {
        val (action, additionalMetaState, keyEventAction) = performActionModel

        ctx.apply {
            when (action.type) {
                ActionType.APP -> {
                    val packageName = action.data

                    val leanbackIntent =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                packageManager.getLeanbackLaunchIntentForPackage(packageName)
                            } else {
                                null
                            }

                    val normalIntent = packageManager.getLaunchIntentForPackage(packageName)

                    val intent = leanbackIntent ?: normalIntent

                    //intent = null if the app doesn't exist
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        try {
                            val appInfo = ctx.packageManager.getApplicationInfo(packageName, 0)

                            //if the app is disabled, show an error message because it won't open
                            if (!appInfo.enabled) {
                                AppDisabled(packageName)
                            }

                            Success(packageName)

                        } catch (e: Exception) {
                            AppNotFound(packageName)
                        }.onFailure {
                            toast(it.getFullMessage(this))
                        }
                    }
                }

                ActionType.APP_SHORTCUT -> {
                    val intent = Intent.parseUri(action.data, 0)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.error_shortcut_not_found)
                    } catch (e: SecurityException) {
                        toast(R.string.error_keymapper_doesnt_have_permission_app_shortcut)
                    } catch (e: Exception) {
                        toast(R.string.error_opening_app_shortcut)
                    }
                }

                ActionType.TEXT_BLOCK -> chosenImePackageName?.let {
                    KeyboardUtils.inputTextFromImeService(ctx, it, action.data)
                }

                ActionType.URL -> {
                    val guessedUrl = URLUtil.guessUrl(action.data)
                    val uri: Uri = Uri.parse(guessedUrl)

                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        toast(R.string.error_no_app_found_to_open_url)
                    }
                }

                ActionType.SYSTEM_ACTION -> performSystemAction(
                        action,
                        chosenImePackageName,
                        currentPackageName
                )

                ActionType.TAP_COORDINATE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val x = action.data.split(',')[0]
                        val y = action.data.split(',')[1]

                        if (AccessibilityUtils.isTapperServiceEnabled(this)) {
                            TapperAccessibilityService.sendTapGesture(
                                    this,
                                    TapGesture(
                                            action,
                                            keyEventAction,
                                            x.toFloat(), y.toFloat()
                                    )
                            )
                        }

//                        val duration = 1L //ms
//
//                        val path = Path().apply {
//                            moveTo(x.toFloat(), y.toFloat())
//                        }
//
//                        val strokeDescription = if (action.flags.hasFlag(Action.ACTION_FLAG_HOLD_DOWN)
//                                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//
//                            when (keyEventAction) {
//                                KeyEventAction.DOWN -> GestureDescription.StrokeDescription(path, 0, duration, true)
//                                KeyEventAction.UP -> GestureDescription.StrokeDescription(path, 59999, duration, false)
//                                else -> null
//                            }
//
//                        } else {
//                            GestureDescription.StrokeDescription(path, 0, duration)
//                        }
//
//                        strokeDescription?.let {
//                            val gestureDescription = GestureDescription.Builder().apply {
//                                addStroke(it)
//                            }.build()
//
//                            dispatchGesture(gestureDescription, null, null)
//                        }
                    }
                }

                ActionType.KEY_EVENT -> {
                    val useShell = action.extras.getData(Action.EXTRA_KEY_EVENT_USE_SHELL)
                            .valueOrNull()
                            .toBoolean()

                    if (useShell) {
                        val keyCode = action.data
                        suProcessDelegate.runCommand("input keyevent $keyCode")
                    }

                    val deviceId = action.extras
                            .getData(Action.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR)
                            .handle(
                                    onSuccess = { InputDeviceUtils.getDeviceIdFromDescriptor(it) },
                                    onFailure = { 0 }
                            )

                    if (PermissionUtils.canUseShizuku(this)) {
                        performKeyActionThroughShizuku(
                                ImitateButtonPress(
                                        keyCode = action.data.toInt(),
                                        metaState = additionalMetaState.withFlag(
                                                action.extras.getData(Action.EXTRA_KEY_EVENT_META_STATE)
                                                        .valueOrNull()
                                                        ?.toInt()
                                                        ?: 0),
                                        keyEventAction = keyEventAction,
                                        deviceId = deviceId ?: 0
                                )
                        )
                    } else {
                        chosenImePackageName?.let {
                            KeyboardUtils.inputKeyEventFromImeService(
                                    ctx,
                                    it,
                                    keyCode = action.data.toInt(),
                                    metaState = additionalMetaState.withFlag(
                                            action.extras.getData(Action.EXTRA_KEY_EVENT_META_STATE)
                                                    .valueOrNull()
                                                    ?.toInt()
                                                    ?: 0
                                    ),
                                    keyEventAction = keyEventAction,
                                    deviceId = deviceId ?: 0)
                        }
                    }
                }

                ActionType.INTENT -> {
                    val intent = Intent.parseUri(action.data, 0)

                    try {
                        action.extras.getData(Action.EXTRA_INTENT_TARGET).onSuccess {
                            when (IntentTarget.valueOf(it)) {
                                IntentTarget.ACTIVITY -> {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                }
                                IntentTarget.BROADCAST_RECEIVER -> sendBroadcast(intent)
                                IntentTarget.SERVICE -> startService(intent)
                            }
                        }
                    } catch (e: Exception) {
                        longToast(e.message!!)
                    }
                }

                ActionType.PHONE_CALL -> {
                    Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:0987654321")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(this)
                    }
                }
            }
        }
    }

    fun performSystemAction(
            id: String,
            chosenImePackageName: String?,
            currentPackageName: String?
    ) = performSystemAction(
            Action(ActionType.SYSTEM_ACTION, id),
            chosenImePackageName,
            currentPackageName
    )

    private fun performSystemAction(action: Action,
                                    chosenImePackageName: String?,
                                    currentPackageName: String?) {

        val id = action.data

        fun getSdkValueForOption(onSuccess: (sdkOptionValue: Int) -> Unit) {
            val extraId = Option.getExtraIdForOption(id)

            action.extras.getData(extraId).onSuccess { option ->
                val sdkOptionValue = Option.OPTION_ID_SDK_ID_MAP[option]

                if (sdkOptionValue != null) {
                    onSuccess(sdkOptionValue)
                }
            }
        }

        fun getSdkValuesForOptionSet(onSuccess: (values: List<Int>) -> Unit) {
            val extraId = Option.getExtraIdForOption(id)

            action.extras.getData(extraId).onSuccess { data ->
                val optionIds = data.split(',')

                val sdkValues = optionIds.map { Option.OPTION_ID_SDK_ID_MAP[it] }

                if (sdkValues.all { it != null }) {
                    onSuccess(sdkValues.map { it!! })
                }
            }
        }

        val showVolumeUi = action.flags.hasFlag(Action.ACTION_FLAG_SHOW_VOLUME_UI)

        ctx.apply {
            when (id) {
                SystemAction.ENABLE_WIFI -> NetworkUtils.changeWifiStatePreQ(this, StateChange.ENABLE)
                SystemAction.DISABLE_WIFI -> NetworkUtils.changeWifiStatePreQ(this, StateChange.DISABLE)
                SystemAction.TOGGLE_WIFI -> NetworkUtils.changeWifiStatePreQ(this, StateChange.TOGGLE)

                SystemAction.TOGGLE_WIFI_ROOT -> NetworkUtils.toggleWifiRoot()
                SystemAction.ENABLE_WIFI_ROOT -> NetworkUtils.enableWifiRoot()
                SystemAction.DISABLE_WIFI_ROOT -> NetworkUtils.disableWifiRoot()

                SystemAction.TOGGLE_BLUETOOTH -> BluetoothUtils.changeBluetoothState(StateChange.TOGGLE)
                SystemAction.ENABLE_BLUETOOTH -> BluetoothUtils.changeBluetoothState(StateChange.ENABLE)
                SystemAction.DISABLE_BLUETOOTH -> BluetoothUtils.changeBluetoothState(StateChange.DISABLE)

                SystemAction.TOGGLE_MOBILE_DATA -> NetworkUtils.toggleMobileData(this)
                SystemAction.ENABLE_MOBILE_DATA -> NetworkUtils.enableMobileData()
                SystemAction.DISABLE_MOBILE_DATA -> NetworkUtils.disableMobileData()

                SystemAction.TOGGLE_AUTO_BRIGHTNESS -> BrightnessUtils.toggleAutoBrightness(this)
                SystemAction.ENABLE_AUTO_BRIGHTNESS ->
                    BrightnessUtils.setBrightnessMode(this, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)

                SystemAction.DISABLE_AUTO_BRIGHTNESS ->
                    BrightnessUtils.setBrightnessMode(this, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)

                SystemAction.INCREASE_BRIGHTNESS -> BrightnessUtils.increaseBrightness(this)
                SystemAction.DECREASE_BRIGHTNESS -> BrightnessUtils.decreaseBrightness(this)

                SystemAction.TOGGLE_AUTO_ROTATE -> ScreenRotationUtils.toggleAutoRotate(this)
                SystemAction.ENABLE_AUTO_ROTATE -> ScreenRotationUtils.enableAutoRotate(this)
                SystemAction.DISABLE_AUTO_ROTATE -> ScreenRotationUtils.disableAutoRotate(this)
                SystemAction.PORTRAIT_MODE -> ScreenRotationUtils.forcePortraitMode(this)
                SystemAction.LANDSCAPE_MODE -> ScreenRotationUtils.forceLandscapeMode(this)
                SystemAction.SWITCH_ORIENTATION -> ScreenRotationUtils.switchOrientation(this)

                SystemAction.CYCLE_ROTATIONS -> getSdkValuesForOptionSet {
                    ScreenRotationUtils.cycleRotations(this, it)
                }

                SystemAction.VOLUME_UP -> AudioUtils.adjustVolume(this, AudioManager.ADJUST_RAISE, showVolumeUi)
                SystemAction.VOLUME_DOWN -> AudioUtils.adjustVolume(this, AudioManager.ADJUST_LOWER, showVolumeUi)

                //the volume UI should always be shown for this action
                SystemAction.VOLUME_SHOW_DIALOG -> AudioUtils.adjustVolume(this, AudioManager.ADJUST_SAME, true)

                SystemAction.VOLUME_DECREASE_STREAM -> getSdkValueForOption { stream ->
                    AudioUtils.adjustSpecificStream(
                            this,
                            AudioManager.ADJUST_LOWER,
                            showVolumeUi,
                            stream
                    )
                }

                SystemAction.VOLUME_INCREASE_STREAM -> getSdkValueForOption { stream ->
                    AudioUtils.adjustSpecificStream(
                            this,
                            AudioManager.ADJUST_RAISE,
                            showVolumeUi,
                            stream
                    )
                }

                SystemAction.CYCLE_VIBRATE_RING -> AudioUtils.cycleBetweenVibrateAndRing(this)
                SystemAction.CYCLE_RINGER_MODE -> AudioUtils.cycleThroughAllRingerModes(this)

                SystemAction.CHANGE_RINGER_MODE -> getSdkValueForOption { ringerMode ->
                    AudioUtils.changeRingerMode(this, ringerMode)
                }

                SystemAction.EXPAND_NOTIFICATION_DRAWER -> StatusBarUtils.expandNotificationDrawer()
                SystemAction.TOGGLE_NOTIFICATION_DRAWER ->
                    currentPackageName?.let { StatusBarUtils.toggleNotificationDrawer(it) }
                SystemAction.EXPAND_QUICK_SETTINGS -> StatusBarUtils.expandQuickSettings()
                SystemAction.TOGGLE_QUICK_SETTINGS_DRAWER ->
                    currentPackageName?.let { StatusBarUtils.toggleQuickSettingsDrawer(it) }
                SystemAction.COLLAPSE_STATUS_BAR -> StatusBarUtils.collapseStatusBar()

                SystemAction.ENABLE_NFC -> NfcUtils.enable()
                SystemAction.DISABLE_NFC -> NfcUtils.disable()
                SystemAction.TOGGLE_NFC -> NfcUtils.toggle(this)

                SystemAction.GO_BACK -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                SystemAction.GO_HOME -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                SystemAction.OPEN_RECENTS -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                SystemAction.OPEN_MENU -> {
                    if (ctx.globalPreferences.hasRootPermission.firstBlocking()) {

                        suProcessDelegate.runCommand("input keyevent ${KeyEvent.KEYCODE_MENU}\n")
                    } else {
                        rootNode.findNodeRecursively {
                            it.contentDescription == OVERFLOW_MENU_CONTENT_DESCRIPTION
                        }?.let {
                            it.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                            it.recycle()
                        }
                    }
                }

                SystemAction.GO_LAST_APP -> {
                    runBlocking {
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

                        delay(100)
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                    }
                }

                SystemAction.OPEN_VOICE_ASSISTANT -> {
                    val intent = Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }

                SystemAction.OPEN_DEVICE_ASSISTANT -> {
                    Intent(Intent.ACTION_ASSIST).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(this)
                    }
                }

                SystemAction.OPEN_CAMERA -> {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }

                SystemAction.LOCK_DEVICE_ROOT ->
                    suProcessDelegate.runCommand("input keyevent ${KeyEvent.KEYCODE_POWER}")

                SystemAction.SHOW_KEYBOARD_PICKER, SystemAction.SHOW_KEYBOARD_PICKER_ROOT ->
                    KeyboardUtils.showInputMethodPickerDialogOutsideApp(ctx)

                SystemAction.SECURE_LOCK_DEVICE -> {
                    val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    dpm.lockNow()
                }

                SystemAction.POWER_ON_OFF_DEVICE -> {
                    suProcessDelegate.runCommand("input keyevent ${KeyEvent.KEYCODE_POWER}")
                }

                SystemAction.MOVE_CURSOR_TO_END -> {
                    if (PermissionUtils.canUseShizuku(this)) {
                        performKeyActionThroughShizuku(
                                ImitateButtonPress(
                                        keyCode = KeyEvent.KEYCODE_MOVE_END,
                                        metaState = KeyEvent.META_CTRL_ON,
                                        deviceId = 0,
                                        keyEventAction = KeyEventAction.DOWN_UP
                                )
                        )
                    } else {
                        chosenImePackageName?.let {
                            KeyboardUtils.inputKeyEventFromImeService(
                                    ctx,
                                    it,
                                    keyCode = KeyEvent.KEYCODE_MOVE_END,
                                    metaState = KeyEvent.META_CTRL_ON,
                                    deviceId = 0
                            )
                        }
                    }
                }

                SystemAction.OPEN_SETTINGS -> {
                    Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(this)
                    }
                }

                SystemAction.SWITCH_KEYBOARD -> {
                    action.extras.getData(Action.EXTRA_IME_ID).onSuccess {
                        KeyboardUtils.switchIme(this, it)
                    }
                }

                SystemAction.TOGGLE_AIRPLANE_MODE -> AirplaneModeUtils.toggleAirplaneMode(this)
                SystemAction.ENABLE_AIRPLANE_MODE -> AirplaneModeUtils.enableAirplaneMode(this)
                SystemAction.DISABLE_AIRPLANE_MODE -> AirplaneModeUtils.disableAirplaneMode(this)

                SystemAction.SCREENSHOT_ROOT -> ScreenshotUtils.takeScreenshotRoot()

                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        when (id) {
                            SystemAction.TEXT_CUT ->
                                rootNode.performActionOnFocusedNode(AccessibilityNodeInfo.ACTION_CUT)

                            SystemAction.TEXT_COPY ->
                                rootNode.performActionOnFocusedNode(AccessibilityNodeInfo.ACTION_COPY)

                            SystemAction.TEXT_PASTE ->
                                rootNode.performActionOnFocusedNode(AccessibilityNodeInfo.ACTION_PASTE)

                            SystemAction.SELECT_WORD_AT_CURSOR -> {
                                rootNode.focusedNode {
                                    it ?: return@focusedNode

                                    //it is the cursor position if they both return the same value
                                    if (it.textSelectionStart == it.textSelectionEnd) {
                                        val cursorPosition = it.textSelectionStart

                                        val wordBoundary =
                                                it.text.toString().getWordBoundaries(cursorPosition)
                                                        ?: return@focusedNode

                                        val bundle = bundleOf(
                                                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT
                                                        to wordBoundary.first,

                                                //The index of the cursor is the index of the last char in the word + 1
                                                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT
                                                        to wordBoundary.second + 1
                                        )

                                        it.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bundle)
                                    }
                                }
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        when (id) {
                            SystemAction.PAUSE_MEDIA -> MediaUtils.pauseMediaPlayback(this)
                            SystemAction.PLAY_MEDIA -> MediaUtils.playMedia(this)
                            SystemAction.PLAY_PAUSE_MEDIA -> MediaUtils.playPauseMediaPlayback(this)
                            SystemAction.NEXT_TRACK -> MediaUtils.nextTrack(this)
                            SystemAction.PREVIOUS_TRACK -> MediaUtils.previousTrack(this)
                            SystemAction.FAST_FORWARD -> MediaUtils.fastForward(this)
                            SystemAction.REWIND -> MediaUtils.rewind(this)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        when (id) {
                            SystemAction.SHOW_POWER_MENU ->
                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)

                            SystemAction.PLAY_MEDIA_PACKAGE -> {
                                action.extras.getData(Action.EXTRA_PACKAGE_NAME).onSuccess {
                                    MediaUtils.playMediaForPackage(ctx, it)
                                }
                            }
                            SystemAction.PLAY_PAUSE_MEDIA_PACKAGE -> {
                                action.extras.getData(Action.EXTRA_PACKAGE_NAME).onSuccess {
                                    MediaUtils.playPauseMediaPlaybackForPackage(ctx, it)
                                }
                            }
                            SystemAction.PAUSE_MEDIA_PACKAGE -> {
                                action.extras.getData(Action.EXTRA_PACKAGE_NAME).onSuccess {
                                    MediaUtils.pauseMediaForPackage(ctx, it)
                                }
                            }
                            SystemAction.NEXT_TRACK_PACKAGE -> {
                                action.extras.getData(Action.EXTRA_PACKAGE_NAME).onSuccess {
                                    MediaUtils.nextTrackForPackage(ctx, it)
                                }
                            }
                            SystemAction.PREVIOUS_TRACK_PACKAGE -> {
                                action.extras.getData(Action.EXTRA_PACKAGE_NAME).onSuccess {
                                    MediaUtils.previousTrackForPackage(ctx, it)
                                }
                            }
                            SystemAction.FAST_FORWARD_PACKAGE -> {
                                action.extras.getData(Action.EXTRA_PACKAGE_NAME).onSuccess {
                                    MediaUtils.fastForwardForPackage(ctx, it)
                                }
                            }
                            SystemAction.REWIND_PACKAGE -> {
                                action.extras.getData(Action.EXTRA_PACKAGE_NAME).onSuccess {
                                    MediaUtils.rewindForPackage(ctx, it)
                                }
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        var lensFacing = CameraCharacteristics.LENS_FACING_BACK

                        action.extras.getData(Action.EXTRA_LENS).onSuccess {
                            val sdkLensFacing = Option.OPTION_ID_SDK_ID_MAP[it]!!

                            lensFacing = sdkLensFacing
                        }

                        when (id) {
                            SystemAction.VOLUME_UNMUTE -> AudioUtils.adjustVolume(
                                    this,
                                    AudioManager.ADJUST_UNMUTE,
                                    showVolumeUi
                            )

                            SystemAction.VOLUME_MUTE -> AudioUtils.adjustVolume(
                                    this,
                                    AudioManager.ADJUST_MUTE,
                                    showVolumeUi
                            )

                            SystemAction.VOLUME_TOGGLE_MUTE ->
                                AudioUtils.adjustVolume(this, AudioManager.ADJUST_TOGGLE_MUTE, showVolumeUi)

                            SystemAction.TOGGLE_FLASHLIGHT -> flashlightController.toggleFlashlight(lensFacing)
                            SystemAction.ENABLE_FLASHLIGHT -> flashlightController.setFlashlightMode(true, lensFacing)
                            SystemAction.DISABLE_FLASHLIGHT -> flashlightController.setFlashlightMode(false, lensFacing)

                            SystemAction.TOGGLE_DND_MODE,
                            SystemAction.ENABLE_DND_MODE -> {
                                action.extras.getData(Action.EXTRA_DND_MODE).onSuccess {
                                    val mode = Option.OPTION_ID_SDK_ID_MAP[it] ?: return@onSuccess

                                    when (id) {
                                        SystemAction.TOGGLE_DND_MODE -> AudioUtils.toggleDndMode(mode)
                                        SystemAction.ENABLE_DND_MODE -> AudioUtils.enableDndMode(mode)
                                    }
                                }
                            }

                            SystemAction.DISABLE_DND_MODE -> AudioUtils.disableDnd()
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        when (id) {
                            SystemAction.TOGGLE_KEYBOARD -> keyboardController?.toggle(this)
                            SystemAction.SHOW_KEYBOARD -> keyboardController?.show(this)
                            SystemAction.HIDE_KEYBOARD -> keyboardController?.hide(this)

                            SystemAction.TOGGLE_SPLIT_SCREEN ->
                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        when (id) {
                            SystemAction.SCREENSHOT ->
                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)

                            SystemAction.LOCK_DEVICE ->
                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    @RequiresApi(Build.VERSION_CODES.M)
    fun performKeyActionThroughShizuku(
            event: ImitateButtonPress
    ) {
        val im = Class.forName("android.hardware.input.IInputManager\$Stub")
                .getDeclaredMethod("asInterface", IBinder::class.java)
                .invoke(null, ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.INPUT_SERVICE)))

        val injectInputEventMethod = im::class.java.getDeclaredMethod(
                "injectInputEvent", InputEvent::class.java, Int::class.java
        )

        val now = SystemClock.uptimeMillis()

        val firstEvent = KeyEvent(
                now, now,
                if (event.keyEventAction == KeyEventAction.UP) KeyEvent.ACTION_UP else KeyEvent.ACTION_DOWN,
                event.keyCode,
                0,
                event.metaState,
                event.deviceId,
                event.scanCode
        )

        //2 = InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH
        injectInputEventMethod.invoke(im, firstEvent, 2)

        if (event.keyEventAction == KeyEventAction.DOWN_UP) {
            //2 = InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH
            injectInputEventMethod.invoke(
                    im,
                    KeyEvent.changeAction(firstEvent, KeyEvent.ACTION_UP),
                    2
            )
        }
    }
}