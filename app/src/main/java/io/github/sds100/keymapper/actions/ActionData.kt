package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.actions.swipegesture.SerializablePath
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.intents.IntentTarget
import io.github.sds100.keymapper.system.volume.DndMode
import io.github.sds100.keymapper.system.volume.RingerMode
import io.github.sds100.keymapper.system.volume.VolumeStream
import kotlinx.serialization.Serializable

@Serializable
sealed class ActionData {
    abstract val id: ActionId
}

@Serializable
data class OpenAppAction(
    val packageName: String
) : ActionData() {
    override val id: ActionId = ActionId.APP
}

@Serializable
data class OpenAppShortcutAction(
    val packageName: String?,
    val shortcutTitle: String,
    val uri: String
) : ActionData() {
    override val id: ActionId = ActionId.APP_SHORTCUT
}

@Serializable
data class KeyEventAction(
    val keyCode: Int,
    val metaState: Int = 0,
    val useShell: Boolean = false,
    val device: Device? = null
) : ActionData() {

    override val id: ActionId = ActionId.KEY_EVENT

    @Serializable
    data class Device(
        val descriptor: String,
        val name: String
    )
}

@Serializable
data class SoundAction(
    val soundUid: String,
    val soundDescription: String
) : ActionData() {
    override val id = ActionId.SOUND
}

@Serializable
sealed class VolumeAction : ActionData() {
    sealed class Stream : VolumeAction() {
        abstract val volumeStream: VolumeStream
        abstract val showVolumeUi: Boolean

        @Serializable
        data class Increase(
            override val showVolumeUi: Boolean,
            override val volumeStream: VolumeStream
        ) : Stream() {
            override val id = ActionId.VOLUME_INCREASE_STREAM
        }

        @Serializable
        data class Decrease(
            override val showVolumeUi: Boolean,
            override val volumeStream: VolumeStream
        ) : Stream() {
            override val id = ActionId.VOLUME_DECREASE_STREAM
        }
    }

    @Serializable
    data class Up(val showVolumeUi: Boolean) : VolumeAction() {
        override val id = ActionId.VOLUME_UP
    }

    @Serializable
    data class Down(val showVolumeUi: Boolean) : VolumeAction() {
        override val id = ActionId.VOLUME_DOWN
    }

    @Serializable
    data class Mute(val showVolumeUi: Boolean) : VolumeAction() {
        override val id = ActionId.VOLUME_MUTE
    }

    @Serializable
    data class UnMute(val showVolumeUi: Boolean) : VolumeAction() {
        override val id = ActionId.VOLUME_UNMUTE
    }

    @Serializable
    data class ToggleMute(val showVolumeUi: Boolean) : VolumeAction() {
        override val id = ActionId.VOLUME_TOGGLE_MUTE
    }

    @Serializable
    data class SetRingerMode(
        val ringerMode: RingerMode
    ) : VolumeAction() {
        override val id: ActionId = ActionId.CHANGE_RINGER_MODE
    }

    @Serializable
    object ShowDialog : VolumeAction() {
        override val id = ActionId.VOLUME_SHOW_DIALOG
    }

    @Serializable
    object CycleRingerMode : VolumeAction() {
        override val id = ActionId.CYCLE_RINGER_MODE
    }

    @Serializable
    object CycleVibrateRing : VolumeAction() {
        override val id = ActionId.CYCLE_VIBRATE_RING
    }
}

@Serializable
sealed class FlashlightAction : ActionData() {
    abstract val lens: CameraLens

    @Serializable
    data class Toggle(override val lens: CameraLens) : FlashlightAction() {
        override val id = ActionId.TOGGLE_FLASHLIGHT
    }

    @Serializable
    data class Enable(override val lens: CameraLens) : FlashlightAction() {
        override val id = ActionId.ENABLE_FLASHLIGHT
    }

    @Serializable
    data class Disable(override val lens: CameraLens) : FlashlightAction() {
        override val id = ActionId.DISABLE_FLASHLIGHT
    }
}

@Serializable
data class SwitchKeyboardAction(
    val imeId: String,
    val savedImeName: String
) : ActionData() {
    override val id = ActionId.SWITCH_KEYBOARD
}

@Serializable
sealed class DndModeAction : ActionData() {

    @Serializable
    data class Toggle(val dndMode: DndMode) : DndModeAction() {
        override val id: ActionId = ActionId.TOGGLE_DND_MODE
    }

    @Serializable
    data class Enable(val dndMode: DndMode) : DndModeAction() {
        override val id: ActionId = ActionId.ENABLE_DND_MODE
    }

    @Serializable
    object Disable : DndModeAction() {
        override val id = ActionId.DISABLE_DND_MODE
    }
}

@Serializable
sealed class RotationAction : ActionData() {
    @Serializable
    object EnableAuto : RotationAction() {
        override val id = ActionId.ENABLE_AUTO_ROTATE
    }

    @Serializable
    object DisableAuto : RotationAction() {
        override val id = ActionId.DISABLE_AUTO_ROTATE
    }

    @Serializable
    object ToggleAuto : RotationAction() {
        override val id = ActionId.TOGGLE_AUTO_ROTATE
    }

    @Serializable
    object Portrait : RotationAction() {
        override val id = ActionId.PORTRAIT_MODE
    }

    @Serializable
    object Landscape : RotationAction() {
        override val id = ActionId.LANDSCAPE_MODE
    }

    @Serializable
    object SwitchOrientation : RotationAction() {
        override val id = ActionId.SWITCH_ORIENTATION
    }

    @Serializable
    data class CycleRotations(
        val orientations: List<Orientation>
    ) : RotationAction() {
        override val id = ActionId.CYCLE_ROTATIONS
    }
}

@Serializable
sealed class ControlMediaForAppAction : ActionData() {
    abstract val packageName: String

    @Serializable
    data class Pause(override val packageName: String) : ControlMediaForAppAction() {
        override val id = ActionId.PAUSE_MEDIA_PACKAGE
    }

    @Serializable
    data class Play(override val packageName: String) : ControlMediaForAppAction() {
        override val id = ActionId.PLAY_MEDIA_PACKAGE
    }

    @Serializable
    data class PlayPause(override val packageName: String) : ControlMediaForAppAction() {
        override val id = ActionId.PLAY_PAUSE_MEDIA_PACKAGE
    }

    @Serializable
    data class NextTrack(override val packageName: String) : ControlMediaForAppAction() {
        override val id = ActionId.NEXT_TRACK_PACKAGE
    }

    @Serializable
    data class PreviousTrack(override val packageName: String) : ControlMediaForAppAction() {
        override val id = ActionId.PREVIOUS_TRACK_PACKAGE
    }

    @Serializable
    data class FastForward(override val packageName: String) : ControlMediaForAppAction() {
        override val id = ActionId.FAST_FORWARD_PACKAGE
    }

    @Serializable
    data class Rewind(override val packageName: String) : ControlMediaForAppAction() {
        override val id = ActionId.REWIND_PACKAGE
    }
}


@Serializable
sealed class ControlMediaAction : ActionData() {
    @Serializable
    object Pause : ControlMediaAction() {
        override val id = ActionId.PAUSE_MEDIA
    }

    @Serializable
    object Play : ControlMediaAction() {
        override val id = ActionId.PLAY_MEDIA
    }

    @Serializable
    object PlayPause : ControlMediaAction() {
        override val id = ActionId.PLAY_PAUSE_MEDIA
    }

    @Serializable
    object NextTrack : ControlMediaAction() {
        override val id = ActionId.NEXT_TRACK
    }

    @Serializable
    object PreviousTrack : ControlMediaAction() {
        override val id = ActionId.PREVIOUS_TRACK
    }

    @Serializable
    object FastForward : ControlMediaAction() {
        override val id = ActionId.FAST_FORWARD
    }

    @Serializable
    object Rewind : ControlMediaAction() {
        override val id = ActionId.REWIND
    }
}

@Serializable
data class IntentAction(
    val description: String,
    val target: IntentTarget,
    val uri: String
) : ActionData() {
    override val id = ActionId.INTENT
}

@Serializable
data class TapCoordinateAction(
    val x: Int,
    val y: Int,
    val description: String?
) : ActionData() {
    override val id = ActionId.TAP_SCREEN
}

@Serializable
data class SwipeGestureAction(
    val path: SerializablePath,
    val description: String?
) : ActionData() {
    override val id = ActionId.SWIPE_SCREEN
}

@Serializable
data class PhoneCallAction(
    val number: String
) : ActionData() {
    override val id = ActionId.PHONE_CALL
}

@Serializable
data class UrlAction(
    val url: String
) : ActionData() {
    override val id = ActionId.URL
}

@Serializable
data class TextAction(
    val text: String
) : ActionData() {
    override val id = ActionId.TEXT
}

@Serializable
sealed class WifiAction : ActionData() {
    @Serializable
    object Enable : WifiAction() {
        override val id = ActionId.ENABLE_WIFI
    }

    @Serializable
    object Disable : WifiAction() {
        override val id = ActionId.DISABLE_WIFI
    }

    @Serializable
    object Toggle : WifiAction() {
        override val id = ActionId.TOGGLE_WIFI
    }
}

@Serializable
sealed class BluetoothAction : ActionData() {
    @Serializable
    object Enable : BluetoothAction() {
        override val id = ActionId.ENABLE_BLUETOOTH
    }

    @Serializable
    object Disable : BluetoothAction() {
        override val id = ActionId.DISABLE_BLUETOOTH
    }

    @Serializable
    object Toggle : BluetoothAction() {
        override val id = ActionId.TOGGLE_BLUETOOTH
    }
}

@Serializable
sealed class NfcAction : ActionData() {
    @Serializable
    object Enable : NfcAction() {
        override val id = ActionId.ENABLE_NFC
    }

    @Serializable
    object Disable : NfcAction() {
        override val id = ActionId.DISABLE_NFC
    }

    @Serializable
    object Toggle : NfcAction() {
        override val id = ActionId.TOGGLE_NFC
    }
}

@Serializable
sealed class AirplaneModeAction : ActionData() {
    @Serializable
    object Enable : AirplaneModeAction() {
        override val id = ActionId.ENABLE_AIRPLANE_MODE
    }

    @Serializable
    object Disable : AirplaneModeAction() {
        override val id = ActionId.DISABLE_AIRPLANE_MODE
    }

    @Serializable
    object Toggle : AirplaneModeAction() {
        override val id = ActionId.TOGGLE_AIRPLANE_MODE
    }
}

@Serializable
sealed class MobileDataAction : ActionData() {
    @Serializable
    object Enable : MobileDataAction() {
        override val id = ActionId.ENABLE_MOBILE_DATA
    }

    @Serializable
    object Disable : MobileDataAction() {
        override val id = ActionId.DISABLE_MOBILE_DATA
    }

    @Serializable
    object Toggle : MobileDataAction() {
        override val id = ActionId.TOGGLE_MOBILE_DATA
    }
}

@Serializable
sealed class BrightnessAction : ActionData() {
    @Serializable
    object EnableAuto : BrightnessAction() {
        override val id = ActionId.ENABLE_AUTO_BRIGHTNESS
    }

    @Serializable
    object DisableAuto : BrightnessAction() {
        override val id = ActionId.DISABLE_AUTO_BRIGHTNESS
    }

    @Serializable
    object ToggleAuto : BrightnessAction() {
        override val id = ActionId.TOGGLE_AUTO_BRIGHTNESS
    }

    @Serializable
    object Increase : BrightnessAction() {
        override val id = ActionId.INCREASE_BRIGHTNESS
    }

    @Serializable
    object Decrease : BrightnessAction() {
        override val id = ActionId.DECREASE_BRIGHTNESS
    }
}

@Serializable
sealed class StatusBarAction : ActionData() {
    @Serializable
    object ExpandNotifications : StatusBarAction() {
        override val id = ActionId.EXPAND_NOTIFICATION_DRAWER
    }

    @Serializable
    object ToggleNotifications : StatusBarAction() {
        override val id = ActionId.TOGGLE_NOTIFICATION_DRAWER
    }

    @Serializable
    object ExpandQuickSettings : StatusBarAction() {
        override val id = ActionId.EXPAND_QUICK_SETTINGS
    }

    @Serializable
    object ToggleQuickSettings : StatusBarAction() {
        override val id = ActionId.TOGGLE_QUICK_SETTINGS
    }

    @Serializable
    object Collapse : StatusBarAction() {
        override val id = ActionId.COLLAPSE_STATUS_BAR
    }
}

@Serializable
object GoBackAction : ActionData() {
    override val id = ActionId.GO_BACK
}

@Serializable
object GoHomeAction : ActionData() {
    override val id = ActionId.GO_HOME
}

@Serializable
object OpenRecentsAction : ActionData() {
    override val id = ActionId.OPEN_RECENTS
}

@Serializable
object GoLastAppAction : ActionData() {
    override val id = ActionId.GO_LAST_APP
}

@Serializable
object OpenMenuAction : ActionData() {
    override val id = ActionId.OPEN_MENU
}

@Serializable
object ToggleSplitScreenAction : ActionData() {
    override val id = ActionId.TOGGLE_SPLIT_SCREEN
}

@Serializable
object ScreenshotAction : ActionData() {
    override val id = ActionId.SCREENSHOT
}

@Serializable
object MoveCursorToEndAction : ActionData() {
    override val id = ActionId.MOVE_CURSOR_TO_END
}

@Serializable
object ToggleKeyboardAction : ActionData() {
    override val id = ActionId.TOGGLE_KEYBOARD
}

@Serializable
object ShowKeyboardAction : ActionData() {
    override val id = ActionId.SHOW_KEYBOARD
}

@Serializable
object HideKeyboardAction : ActionData() {
    override val id = ActionId.HIDE_KEYBOARD
}

@Serializable
object ShowKeyboardPickerAction : ActionData() {
    override val id = ActionId.SHOW_KEYBOARD_PICKER
}

@Serializable
object CopyTextAction : ActionData() {
    override val id = ActionId.TEXT_COPY
}

@Serializable
object PasteTextAction : ActionData() {
    override val id = ActionId.TEXT_PASTE
}

@Serializable
object CutTextAction : ActionData() {
    override val id = ActionId.TEXT_CUT
}

@Serializable
object SelectWordAtCursorAction : ActionData() {
    override val id = ActionId.SELECT_WORD_AT_CURSOR
}

@Serializable
object VoiceAssistantAction : ActionData() {
    override val id = ActionId.OPEN_VOICE_ASSISTANT
}


@Serializable
object DeviceAssistantAction : ActionData() {
    override val id = ActionId.OPEN_DEVICE_ASSISTANT
}

@Serializable
object OpenCameraAction : ActionData() {
    override val id = ActionId.OPEN_CAMERA
}

@Serializable
object LockDeviceAction : ActionData() {
    override val id = ActionId.LOCK_DEVICE
}

@Serializable
object ScreenOnOffAction : ActionData() {
    override val id = ActionId.POWER_ON_OFF_DEVICE
}

@Serializable
object SecureLockAction : ActionData() {
    override val id = ActionId.SECURE_LOCK_DEVICE
}

@Serializable
object ConsumeKeyEventAction : ActionData() {
    override val id = ActionId.CONSUME_KEY_EVENT
}

@Serializable
object OpenSettingsAction : ActionData() {
    override val id = ActionId.OPEN_SETTINGS
}

@Serializable
object ShowPowerMenuAction : ActionData() {
    override val id = ActionId.SHOW_POWER_MENU
}

@Serializable
object DismissLastNotificationAction : ActionData() {
    override val id: ActionId = ActionId.DISMISS_MOST_RECENT_NOTIFICATION
}

@Serializable
object DismissAllNotificationsAction : ActionData() {
    override val id: ActionId = ActionId.DISMISS_ALL_NOTIFICATIONS
}