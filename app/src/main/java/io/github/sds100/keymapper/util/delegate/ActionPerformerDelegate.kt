package io.github.sds100.keymapper.util.delegate

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.DEVICE_POLICY_SERVICE
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.URLUtil
import androidx.core.os.bundleOf
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.lifecycle.Lifecycle
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Option
import io.github.sds100.keymapper.data.model.PerformActionModel
import io.github.sds100.keymapper.data.model.getData
import io.github.sds100.keymapper.service.KeyMapperImeService
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.onSuccess
import splitties.bitflags.hasFlag
import splitties.toast.toast


/**
 * Created by sds100 on 25/11/2018.
 */

class ActionPerformerDelegate(context: Context,
                              iPerformAccessibilityAction: IPerformAccessibilityAction,
                              lifecycle: Lifecycle) : IPerformAccessibilityAction by iPerformAccessibilityAction {

    companion object {
        private const val OVERFLOW_MENU_CONTENT_DESCRIPTION = "More options"
    }

    private val mCtx = context.applicationContext
    private lateinit var mFlashlightController: FlashlightController

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFlashlightController = FlashlightController()
            lifecycle.addObserver(mFlashlightController)
        }
    }

    fun performAction(action: Action) = performAction(PerformActionModel(action))

    fun performAction(performActionModel: PerformActionModel) {
        val (action, showToast, additionalMetaState, keyEventAction) = performActionModel

        mCtx.apply {
            //Only show a toast message that Key Mapper is performing an action if the user has enabled it
            if (showToast) {
                toast(R.string.performing_action)
            }

            when (action.type) {
                ActionType.APP -> {
                    val intent = packageManager.getLaunchIntentForPackage(action.data)

                    //intent = null if the app doesn't exist
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        toast(R.string.error_app_isnt_installed)
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

                ActionType.TEXT_BLOCK -> {
                    KeyMapperImeService.provideBus().value =
                        Event(KeyMapperImeService.EVENT_INPUT_TEXT to action.data)
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

                ActionType.SYSTEM_ACTION -> performSystemAction(action)

                else -> {
                    if (action.type == ActionType.KEY_EVENT) {
                        Intent(Constants.INPUT_METHOD_ACTION_INPUT_DOWN_UP).apply {
                            //put package of the chosen keyboard to improve performance so the intent isn't sent to all apps that have the permission
                            putExtra(Constants.INPUT_METHOD_EXTRA_KEYCODE, action.data.toInt())
                            sendBroadcast(this, Constants.PERMISSION_KEY_MAPPER_INPUT_METHOD)
                        }

//                        KeyboardUtils.sendDownUpFromImeService(
//                            keyCode = action.data.toInt(),
//                            metaState = additionalMetaState.withFlag(
//                                action.extras.getData(Action.EXTRA_KEY_EVENT_META_STATE).valueOrNull()?.toInt() ?: 0
//                            ),
//                            keyEventAction = keyEventAction
//                        )
                    }
                }
            }
        }
    }

    fun performSystemAction(id: String) = performSystemAction(Action(ActionType.SYSTEM_ACTION, id))

    private fun performSystemAction(action: Action) {

        fun getSdkValueForOption(systemActionId: String, onSuccess: (sdkOptionValue: Int) -> Unit) {
            val extraId = Option.getExtraIdForOption(systemActionId)

            action.extras.getData(extraId).onSuccess { option ->
                val sdkOptionValue = Option.OPTION_ID_SDK_ID_MAP[option]

                if (sdkOptionValue != null) {
                    onSuccess(sdkOptionValue)
                }
            }
        }

        val id = action.data

        val showVolumeUi = action.flags.hasFlag(Action.ACTION_FLAG_SHOW_VOLUME_UI)

        mCtx.apply {
            when (id) {
                SystemAction.ENABLE_WIFI -> NetworkUtils.changeWifiState(this, StateChange.ENABLE)
                SystemAction.DISABLE_WIFI -> NetworkUtils.changeWifiState(this, StateChange.DISABLE)
                SystemAction.TOGGLE_WIFI -> NetworkUtils.changeWifiState(this, StateChange.TOGGLE)

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

                SystemAction.VOLUME_UP -> AudioUtils.adjustVolume(this, AudioManager.ADJUST_RAISE, showVolumeUi)
                SystemAction.VOLUME_DOWN -> AudioUtils.adjustVolume(this, AudioManager.ADJUST_LOWER, showVolumeUi)

                //the volume UI should always be shown for this action
                SystemAction.VOLUME_SHOW_DIALOG -> AudioUtils.adjustVolume(this, AudioManager.ADJUST_SAME, true)

                SystemAction.VOLUME_DECREASE_STREAM -> getSdkValueForOption(id) { stream ->
                    AudioUtils.adjustSpecificStream(
                        this,
                        AudioManager.ADJUST_LOWER,
                        showVolumeUi,
                        stream
                    )
                }

                SystemAction.VOLUME_INCREASE_STREAM -> getSdkValueForOption(id) { stream ->
                    AudioUtils.adjustSpecificStream(
                        this,
                        AudioManager.ADJUST_RAISE,
                        showVolumeUi,
                        stream
                    )
                }

                SystemAction.CYCLE_VIBRATE_RING -> AudioUtils.cycleBetweenVibrateAndRing(this)
                SystemAction.CYCLE_RINGER_MODE -> AudioUtils.cycleThroughAllRingerModes(this)

                SystemAction.CHANGE_RINGER_MODE -> getSdkValueForOption(id) { ringerMode ->
                    AudioUtils.changeRingerMode(this, ringerMode)
                }

                SystemAction.EXPAND_NOTIFICATION_DRAWER -> StatusBarUtils.expandNotificationDrawer()
                SystemAction.EXPAND_QUICK_SETTINGS -> StatusBarUtils.expandQuickSettings()
                SystemAction.COLLAPSE_STATUS_BAR -> StatusBarUtils.collapseStatusBar()

                SystemAction.ENABLE_NFC -> NfcUtils.enable()
                SystemAction.DISABLE_NFC -> NfcUtils.disable()
                SystemAction.TOGGLE_NFC -> NfcUtils.toggle(this)

                SystemAction.PAUSE_MEDIA -> MediaUtils.pauseMediaPlayback(this)
                SystemAction.PLAY_MEDIA -> MediaUtils.playMedia(this)
                SystemAction.PLAY_PAUSE_MEDIA -> MediaUtils.playPauseMediaPlayback(this)
                SystemAction.NEXT_TRACK -> MediaUtils.nextTrack(this)
                SystemAction.PREVIOUS_TRACK -> MediaUtils.previousTrack(this)
                SystemAction.FAST_FORWARD -> MediaUtils.fastForward(this)
                SystemAction.REWIND -> MediaUtils.rewind(this)

                SystemAction.GO_BACK -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                SystemAction.GO_HOME -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                SystemAction.OPEN_RECENTS -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                SystemAction.OPEN_MENU -> {
                    if (AppPreferences.hasRootPermission) {
                        RootUtils.executeRootCommand("input keyevent ${KeyEvent.KEYCODE_MENU}")
                    } else {
                        rootNode.findNodeRecursively {
                            it.contentDescription == OVERFLOW_MENU_CONTENT_DESCRIPTION
                        }?.let {
                            it.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                            it.recycle()
                        }
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

                SystemAction.LOCK_DEVICE_ROOT -> RootUtils.executeRootCommand("input keyevent ${KeyEvent.KEYCODE_POWER}")

                SystemAction.SHOW_KEYBOARD_PICKER, SystemAction.SHOW_KEYBOARD_PICKER_ROOT ->
                    KeyboardUtils.showInputMethodPickerDialogOutsideApp()

                SystemAction.SECURE_LOCK_DEVICE -> {
                    val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    dpm.lockNow()
                }

                SystemAction.MOVE_CURSOR_TO_END -> KeyboardUtils.sendDownUpFromImeService(
                    keyCode = KeyEvent.KEYCODE_MOVE_END,
                    metaState = KeyEvent.META_CTRL_ON
                )

                SystemAction.OPEN_SETTINGS -> {
                    Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(this)
                    }
                }

                SystemAction.SWITCH_KEYBOARD -> {
                    action.extras.getData(Action.EXTRA_IME_ID).onSuccess {
                        KeyboardUtils.switchIme(it)
                    }
                }

                SystemAction.TOGGLE_AIRPLANE_MODE -> AirplaneModeUtils.toggleAirplaneMode(this)
                SystemAction.ENABLE_AIRPLANE_MODE -> AirplaneModeUtils.enableAirplaneMode()
                SystemAction.DISABLE_AIRPLANE_MODE -> AirplaneModeUtils.disableAirplaneMode()

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
                                            it.text.toString().getWordBoundaries(cursorPosition) ?: return@focusedNode

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

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        when (id) {
                            SystemAction.SHOW_POWER_MENU ->
                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
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

                            SystemAction.TOGGLE_FLASHLIGHT -> mFlashlightController.toggleFlashlight(lensFacing)
                            SystemAction.ENABLE_FLASHLIGHT -> mFlashlightController.setFlashlightMode(true, lensFacing)
                            SystemAction.DISABLE_FLASHLIGHT -> mFlashlightController.setFlashlightMode(false, lensFacing)

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
}