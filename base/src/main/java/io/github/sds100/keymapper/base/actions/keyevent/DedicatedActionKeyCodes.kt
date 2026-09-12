package io.github.sds100.keymapper.base.actions.keyevent

import android.view.KeyEvent
import io.github.sds100.keymapper.base.actions.ActionData

/**
 * Many users create a "Key Code" action for a key that actually has a dedicated action, expecting
 * it to work like a system shortcut. This does not work for system-level keys
 * (volume, power, home, screenshot, media, etc.) it silently does nothing on most devices.
 * This returns the dedicated [ActionData]
 * Key Mapper recommends instead of injecting [keyCode] directly, or null if there isn't an
 * unambiguous one.
 */
fun getDedicatedKeyCodeAction(keyCode: Int): ActionData? = when (keyCode) {
    KeyEvent.KEYCODE_VOLUME_UP -> ActionData.Volume.Up(showVolumeUi = false)
    KeyEvent.KEYCODE_VOLUME_DOWN -> ActionData.Volume.Down(showVolumeUi = false)
    KeyEvent.KEYCODE_VOLUME_MUTE -> ActionData.Volume.Mute(showVolumeUi = false)
    KeyEvent.KEYCODE_POWER -> ActionData.LockDevice
    KeyEvent.KEYCODE_LANGUAGE_SWITCH -> ActionData.CycleKeyboardLanguage
    KeyEvent.KEYCODE_CAMERA -> ActionData.OpenCamera
    KeyEvent.KEYCODE_SETTINGS -> ActionData.OpenSettings
    KeyEvent.KEYCODE_HOME -> ActionData.GoHome
    KeyEvent.KEYCODE_APP_SWITCH -> ActionData.OpenRecents
    KeyEvent.KEYCODE_BACK -> ActionData.GoBack
    KeyEvent.KEYCODE_MUTE -> ActionData.Microphone.Mute
    KeyEvent.KEYCODE_SCREENSHOT -> ActionData.Screenshot
    KeyEvent.KEYCODE_MEDIA_PLAY -> ActionData.ControlMedia.Play
    KeyEvent.KEYCODE_MEDIA_PAUSE -> ActionData.ControlMedia.Pause
    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> ActionData.ControlMedia.PlayPause
    KeyEvent.KEYCODE_MEDIA_NEXT -> ActionData.ControlMedia.NextTrack
    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> ActionData.ControlMedia.PreviousTrack
    KeyEvent.KEYCODE_MEDIA_STOP -> ActionData.ControlMedia.Stop
    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> ActionData.ControlMedia.FastForward
    KeyEvent.KEYCODE_MEDIA_REWIND -> ActionData.ControlMedia.Rewind
    KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> ActionData.ControlMedia.StepForward
    KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> ActionData.ControlMedia.StepBackward
    KeyEvent.KEYCODE_BRIGHTNESS_UP -> ActionData.Brightness.Increase
    KeyEvent.KEYCODE_BRIGHTNESS_DOWN -> ActionData.Brightness.Decrease
    KeyEvent.KEYCODE_VOICE_ASSIST -> ActionData.VoiceAssistant
    KeyEvent.KEYCODE_ASSIST -> ActionData.DeviceAssistant
    KeyEvent.KEYCODE_CALL -> ActionData.AnswerCall
    KeyEvent.KEYCODE_ENDCALL -> ActionData.EndCall
    KeyEvent.KEYCODE_NOTIFICATION -> ActionData.StatusBar.ExpandNotifications
    else -> null
}
