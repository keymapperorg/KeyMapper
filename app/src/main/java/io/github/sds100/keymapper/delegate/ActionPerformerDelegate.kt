package io.github.sds100.keymapper.delegate

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Context.DEVICE_POLICY_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.lifecycle.Lifecycle
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.interfaces.IContext
import io.github.sds100.keymapper.interfaces.IPerformAccessibilityAction
import io.github.sds100.keymapper.service.MyIMEService
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.FlagUtils.FLAG_SHOW_VOLUME_UI
import io.github.sds100.keymapper.util.FlagUtils.FLAG_VIBRATE
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.toast


/**
 * Created by sds100 on 25/11/2018.
 */

class ActionPerformerDelegate(
    iContext: IContext,
    iPerformAccessibilityAction: IPerformAccessibilityAction,
    lifecycle: Lifecycle
) : IContext by iContext, IPerformAccessibilityAction by iPerformAccessibilityAction {

    companion object {
        private const val OVERFLOW_MENU_CONTENT_DESCRIPTION = "More options"
    }

    private lateinit var mFlashlightController: FlashlightController

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFlashlightController = FlashlightController(this)
            lifecycle.addObserver(mFlashlightController)
        }
    }

    fun performAction(action: Action, flags: Int) {
        ctx.apply {
            //Only show a toast message that Key Mapper is performing an action if the user has enabled it
            val key = str(R.string.key_pref_show_toast_when_action_performed)

            if (defaultSharedPreferences.getBoolean(key, bool(R.bool.default_value_show_toast))) {
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
                    val intent = Intent(MyIMEService.ACTION_INPUT_TEXT)
                    //put the text in the intent
                    intent.putExtra(MyIMEService.EXTRA_TEXT, action.data)

                    sendBroadcast(intent)
                }

                ActionType.SYSTEM_ACTION -> performSystemAction(action, flags)

                else -> {
                    //for actions which require the IME service
                    if (action.type == ActionType.KEYCODE || action.type == ActionType.KEY) {
                        inputKeyCode(action.data.toInt())
                    }
                }
            }
        }
    }

    fun performSystemAction(id: String) = performSystemAction(Action(ActionType.SYSTEM_ACTION, id), 0)

    private fun performSystemAction(action: Action, flags: Int) {

        fun getSdkValueForOption(systemActionId: String, onSuccess: (sdkOptionValue: Int) -> Unit) {
            val extraId = Option.getExtraIdForOption(systemActionId)

            action.getExtraData(extraId).onSuccess { option ->
                val sdkOptionValue = Option.OPTION_ID_SDK_ID_MAP[option]

                if (sdkOptionValue != null) {
                    onSuccess(sdkOptionValue)
                }
            }
        }

        val id = action.data

        val showVolumeUi = containsFlag(flags, FLAG_SHOW_VOLUME_UI)

        ctx.apply {
            if (defaultSharedPreferences.getBoolean(
                    str(R.string.key_pref_force_vibrate),
                    bool(R.bool.default_value_force_vibrate)
                ) or containsFlag(flags, FLAG_VIBRATE)) {

                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                val vibrateDuration = defaultSharedPreferences.getInt(
                    str(R.string.key_pref_vibrate_duration),
                    int(R.integer.default_value_vibrate_duration)
                ).toLong()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(vibrateDuration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(vibrateDuration)
                }
            }

            when (id) {
                SystemAction.ENABLE_WIFI -> NetworkUtils.changeWifiState(this, StateChange.ENABLE)
                SystemAction.DISABLE_WIFI -> NetworkUtils.changeWifiState(this, StateChange.DISABLE)
                SystemAction.TOGGLE_WIFI -> NetworkUtils.changeWifiState(this, StateChange.TOGGLE)

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

                SystemAction.CYCLE_RINGER_MODE -> AudioUtils.cycleThroughRingerModes(this)

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
                    if (RootUtils.checkAppHasRootPermission(this)) {
                        RootUtils.executeRootCommand("input keyevent ${KeyEvent.KEYCODE_MENU}")
                    } else {
                        rootNode.findNodeRecursively {
                            it.contentDescription == OVERFLOW_MENU_CONTENT_DESCRIPTION
                        }?.let {
                            it.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                        }
                    }
                }

                SystemAction.OPEN_ASSISTANT -> {
                    val intent = Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }

                SystemAction.OPEN_CAMERA -> {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }

                SystemAction.LOCK_DEVICE -> RootUtils.executeRootCommand("input keyevent ${KeyEvent.KEYCODE_POWER}")

                SystemAction.SHOW_KEYBOARD_PICKER, SystemAction.SHOW_KEYBOARD_PICKER_ROOT ->
                    KeyboardUtils.showInputMethodPickerDialogOutsideApp(this)

                SystemAction.SECURE_LOCK_DEVICE -> {
                    val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    dpm.lockNow()
                }

                SystemAction.MOVE_CURSOR_TO_END -> inputKeyEvent(
                    keyCode = KeyEvent.KEYCODE_MOVE_END,
                    metaState = KeyEvent.META_CTRL_ON
                )

                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        var lensFacing = CameraCharacteristics.LENS_FACING_BACK

                        action.getExtraData(Action.EXTRA_LENS).onSuccess {
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
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        when (id) {
                            SystemAction.TOGGLE_KEYBOARD -> keyboardController?.toggle(this)
                            SystemAction.SHOW_KEYBOARD -> keyboardController?.show(this)
                            SystemAction.HIDE_KEYBOARD -> keyboardController?.hide(this)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        when (id) {
                            SystemAction.SCREENSHOT ->
                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                        }
                    }
                }
            }
        }
    }
}
