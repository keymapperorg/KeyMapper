package io.github.sds100.keymapper.Delegates

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.Interfaces.IContext
import io.github.sds100.keymapper.Interfaces.IPerformGlobalAction
import io.github.sds100.keymapper.Services.MyIMEService
import io.github.sds100.keymapper.Utils.*
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by sds100 on 25/11/2018.
 */

class PerformActionDelegate(iContext: IContext, iPerformGlobalAction: IPerformGlobalAction
) : IContext by iContext, IPerformGlobalAction by iPerformGlobalAction {

    fun performAction(action: Action) {
        context.apply {
            //Only show a toast message that Key Mapper is performing an action if the user has enabled it
            val key = getString(R.string.key_pref_show_toast_when_action_performed)

            if (defaultSharedPreferences.getBoolean(key, false)) {
                Toast.makeText(this, R.string.performing_action, Toast.LENGTH_SHORT).show()
            }

            when (action.type) {
                ActionType.APP -> {
                    val intent = packageManager.getLaunchIntentForPackage(action.data)

                    //intent = null if the app doesn't exist
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, R.string.error_app_isnt_installed, Toast.LENGTH_SHORT).show()
                    }
                }

                ActionType.APP_SHORTCUT -> {
                    val intent = Intent.parseUri(action.data, 0)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    try {
                        startActivity(intent)
                    } catch (exception: ActivityNotFoundException) {
                        Toast.makeText(this, R.string.error_shortcut_not_found, Toast.LENGTH_SHORT).show()
                    }
                }

                ActionType.TEXT_BLOCK -> {
                    val intent = Intent(MyIMEService.ACTION_INPUT_TEXT)
                    //put the text in the intent
                    intent.putExtra(MyIMEService.EXTRA_TEXT, action.data)

                    sendBroadcast(intent)
                }

                ActionType.SYSTEM_ACTION -> performSystemAction(action.data)

                else -> {
                    //for actions which require the IME service
                    if (action.type == ActionType.KEYCODE || action.type == ActionType.KEY) {
                        val intent = Intent(MyIMEService.ACTION_INPUT_KEYCODE)
                        //put the keycode in the intent
                        intent.putExtra(MyIMEService.EXTRA_KEYCODE, action.data.toInt())

                        sendBroadcast(intent)
                    }
                }
            }
        }
    }

    private fun performSystemAction(id: String) {
        context.apply {
            when (id) {
                SystemAction.ENABLE_WIFI -> WifiUtils.changeWifiState(this, StateChange.ENABLE)
                SystemAction.DISABLE_WIFI -> WifiUtils.changeWifiState(this, StateChange.DISABLE)
                SystemAction.TOGGLE_WIFI -> WifiUtils.changeWifiState(this, StateChange.TOGGLE)

                SystemAction.TOGGLE_BLUETOOTH -> BluetoothUtils.changeBluetoothState(StateChange.TOGGLE)
                SystemAction.ENABLE_BLUETOOTH -> BluetoothUtils.changeBluetoothState(StateChange.ENABLE)
                SystemAction.DISABLE_BLUETOOTH -> BluetoothUtils.changeBluetoothState(StateChange.DISABLE)

                SystemAction.TOGGLE_MOBILE_DATA -> MobileDataUtils.toggleMobileData(this)
                SystemAction.ENABLE_MOBILE_DATA -> MobileDataUtils.enableMobileData()
                SystemAction.DISABLE_MOBILE_DATA -> MobileDataUtils.disableMobileData()

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

                SystemAction.VOLUME_UP -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_RAISE)
                SystemAction.VOLUME_DOWN -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_LOWER)
                SystemAction.VOLUME_SHOW_DIALOG -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_SAME)

                SystemAction.EXPAND_NOTIFICATION_DRAWER -> StatusBarUtils.expandNotificationDrawer()
                SystemAction.EXPAND_QUICK_SETTINGS -> StatusBarUtils.expandQuickSettings()
                SystemAction.COLLAPSE_STATUS_BAR -> StatusBarUtils.collapseStatusBar()

                SystemAction.PAUSE_MEDIA -> MediaUtils.pauseMediaPlayback(this)
                SystemAction.PLAY_MEDIA -> MediaUtils.playMedia(this)
                SystemAction.PLAY_PAUSE_MEDIA -> MediaUtils.playPauseMediaPlayback(this)
                SystemAction.NEXT_TRACK -> MediaUtils.nextTrack(this)
                SystemAction.PREVIOUS_TRACK -> MediaUtils.previousTrack(this)

                SystemAction.GO_BACK -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                SystemAction.GO_HOME -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                SystemAction.OPEN_RECENTS -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

                //there must be a way to do this without root
                SystemAction.OPEN_MENU -> RootUtils.executeRootCommand("input keyevent 82")

                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        when (id) {
                            SystemAction.VOLUME_UNMUTE -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_UNMUTE)
                            SystemAction.VOLUME_MUTE -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_MUTE)
                            SystemAction.VOLUME_TOGGLE_MUTE -> VolumeUtils.adjustVolume(this, AudioManager.ADJUST_TOGGLE_MUTE)
                        }
                    }
                }
            }
        }
    }
}
