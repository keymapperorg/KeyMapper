package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.actions.pinchscreen.PinchScreenType
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.intents.IntentExtraModel
import io.github.sds100.keymapper.system.intents.IntentTarget
import io.github.sds100.keymapper.system.network.HttpMethod
import io.github.sds100.keymapper.system.volume.DndMode
import io.github.sds100.keymapper.system.volume.RingerMode
import io.github.sds100.keymapper.system.volume.VolumeStream
import kotlinx.serialization.Serializable

@Serializable
sealed class ActionData : Comparable<ActionData> {
    abstract val id: ActionId

    override fun compareTo(other: ActionData) = id.compareTo(other.id)

    @Serializable
    data class App(
        val packageName: String,
    ) : ActionData() {
        override val id: ActionId = ActionId.APP

        override fun compareTo(other: ActionData) = when (other) {
            is App -> packageName.compareTo(other.packageName)
            else -> super.compareTo(other)
        }
    }

    @Serializable
    data class AppShortcut(
        val packageName: String?,
        val shortcutTitle: String,
        val uri: String,
    ) : ActionData() {
        override val id: ActionId = ActionId.APP_SHORTCUT

        override fun compareTo(other: ActionData) = when (other) {
            is AppShortcut -> shortcutTitle.compareTo(other.shortcutTitle)
            else -> super.compareTo(other)
        }
    }

    @Serializable
    data class InputKeyEvent(
        val keyCode: Int,
        val metaState: Int = 0,
        val useShell: Boolean = false,
        val device: Device? = null,
    ) : ActionData() {

        override val id: ActionId = ActionId.KEY_EVENT

        @Serializable
        data class Device(
            val descriptor: String,
            val name: String,
        )

        override fun compareTo(other: ActionData) = when (other) {
            is InputKeyEvent -> keyCode.compareTo(other.keyCode)
            else -> super.compareTo(other)
        }
    }

    @Serializable
    data class Sound(
        val soundUid: String,
        val soundDescription: String,
    ) : ActionData() {
        override val id = ActionId.SOUND

        override fun compareTo(other: ActionData) = when (other) {
            is Sound -> soundUid.compareTo(other.soundUid)
            else -> super.compareTo(other)
        }
    }

    @Serializable
    sealed class Volume : ActionData() {
        sealed class Stream : Volume() {
            abstract val volumeStream: VolumeStream
            abstract val showVolumeUi: Boolean

            override fun compareTo(other: ActionData) = when (other) {
                is Stream -> compareValuesBy(
                    this,
                    other,
                    { it.id },
                    { it.volumeStream },
                )

                else -> super.compareTo(other)
            }

            @Serializable
            data class Increase(
                override val showVolumeUi: Boolean,
                override val volumeStream: VolumeStream,
            ) : Stream() {
                override val id = ActionId.VOLUME_INCREASE_STREAM
            }

            @Serializable
            data class Decrease(
                override val showVolumeUi: Boolean,
                override val volumeStream: VolumeStream,
            ) : Stream() {
                override val id = ActionId.VOLUME_DECREASE_STREAM
            }
        }

        @Serializable
        data class Up(val showVolumeUi: Boolean) : Volume() {
            override val id = ActionId.VOLUME_UP
        }

        @Serializable
        data class Down(val showVolumeUi: Boolean) : Volume() {
            override val id = ActionId.VOLUME_DOWN
        }

        @Serializable
        data class Mute(val showVolumeUi: Boolean) : Volume() {
            override val id = ActionId.VOLUME_MUTE
        }

        @Serializable
        data class UnMute(val showVolumeUi: Boolean) : Volume() {
            override val id = ActionId.VOLUME_UNMUTE
        }

        @Serializable
        data class ToggleMute(val showVolumeUi: Boolean) : Volume() {
            override val id = ActionId.VOLUME_TOGGLE_MUTE
        }

        @Serializable
        data class SetRingerMode(
            val ringerMode: RingerMode,
        ) : Volume() {
            override val id: ActionId = ActionId.CHANGE_RINGER_MODE

            override fun compareTo(other: ActionData) = when (other) {
                is SetRingerMode -> ringerMode.compareTo(other.ringerMode)
                else -> super.compareTo(other)
            }
        }

        @Serializable
        data object ShowDialog : Volume() {
            override val id = ActionId.VOLUME_SHOW_DIALOG
        }

        @Serializable
        data object CycleRingerMode : Volume() {
            override val id = ActionId.CYCLE_RINGER_MODE
        }

        @Serializable
        data object CycleVibrateRing : Volume() {
            override val id = ActionId.CYCLE_VIBRATE_RING
        }
    }

    @Serializable
    sealed class Flashlight : ActionData() {
        abstract val lens: CameraLens

        override fun compareTo(other: ActionData) = when (other) {
            is Flashlight -> compareValuesBy(
                this,
                other,
                { it.id },
                { it.lens },
            )

            else -> super.compareTo(other)
        }

        @Serializable
        data class Toggle(
            override val lens: CameraLens,
            /**
             * Strength is null if the default strength should be used. This is a percentage
             * of the flash strength so key maps can be exported to other devices with potentially
             * different strength levels.
             */
            val strengthPercent: Float?,
        ) : Flashlight() {
            override val id = ActionId.TOGGLE_FLASHLIGHT
        }

        @Serializable
        data class Enable(
            override val lens: CameraLens,
            /**
             * Strength is null if the default strength should be used. This is a percentage
             * of the flash strength so key maps can be exported to other devices with potentially
             * different strength levels.
             */
            val strengthPercent: Float?,
        ) : Flashlight() {
            override val id = ActionId.ENABLE_FLASHLIGHT
        }

        @Serializable
        data class Disable(override val lens: CameraLens) : Flashlight() {
            override val id = ActionId.DISABLE_FLASHLIGHT
        }

        @Serializable
        data class ChangeStrength(
            override val lens: CameraLens,
            /**
             * This can be positive or negative to increase/decrease respectively.
             */
            val percent: Float,
        ) : Flashlight() {
            override val id = ActionId.CHANGE_FLASHLIGHT_STRENGTH
        }
    }

    @Serializable
    data class SwitchKeyboard(
        val imeId: String,
        val savedImeName: String,
    ) : ActionData() {
        override val id = ActionId.SWITCH_KEYBOARD

        override fun compareTo(other: ActionData) = when (other) {
            is SwitchKeyboard -> savedImeName.compareTo(other.savedImeName)
            else -> super.compareTo(other)
        }
    }

    @Serializable
    sealed class DoNotDisturb : ActionData() {

        @Serializable
        data class Toggle(val dndMode: DndMode) : DoNotDisturb() {
            override val id: ActionId = ActionId.TOGGLE_DND_MODE

            override fun compareTo(other: ActionData) = when (other) {
                is Toggle -> dndMode.compareTo(other.dndMode)
                else -> super.compareTo(other)
            }
        }

        @Serializable
        data class Enable(val dndMode: DndMode) : DoNotDisturb() {
            override val id: ActionId = ActionId.ENABLE_DND_MODE

            override fun compareTo(other: ActionData) = when (other) {
                is Enable -> dndMode.compareTo(other.dndMode)
                else -> super.compareTo(other)
            }
        }

        @Serializable
        object Disable : DoNotDisturb() {
            override val id = ActionId.DISABLE_DND_MODE
        }
    }

    @Serializable
    sealed class Rotation : ActionData() {
        @Serializable
        object EnableAuto : Rotation() {
            override val id = ActionId.ENABLE_AUTO_ROTATE
        }

        @Serializable
        object DisableAuto : Rotation() {
            override val id = ActionId.DISABLE_AUTO_ROTATE
        }

        @Serializable
        object ToggleAuto : Rotation() {
            override val id = ActionId.TOGGLE_AUTO_ROTATE
        }

        @Serializable
        object Portrait : Rotation() {
            override val id = ActionId.PORTRAIT_MODE
        }

        @Serializable
        object Landscape : Rotation() {
            override val id = ActionId.LANDSCAPE_MODE
        }

        @Serializable
        object SwitchOrientation : Rotation() {
            override val id = ActionId.SWITCH_ORIENTATION
        }

        @Serializable
        data class CycleRotations(
            val orientations: List<Orientation>,
        ) : Rotation() {
            override val id = ActionId.CYCLE_ROTATIONS

            override fun compareTo(other: ActionData) = when (other) {
                // Compare orientations one by one until a difference is found otherwise compare the size
                is CycleRotations -> orientations.zip(other.orientations)
                    .map { (a, b) -> a.compareTo(b) }
                    .firstOrNull { it != 0 }
                    ?: orientations.size.compareTo(other.orientations.size)

                else -> super.compareTo(other)
            }
        }
    }

    @Serializable
    sealed class ControlMediaForApp : ActionData() {
        abstract val packageName: String

        override fun compareTo(other: ActionData) = when (other) {
            is ControlMediaForApp -> compareValuesBy(
                this,
                other,
                { it.id },
                { it.packageName },
            )

            else -> super.compareTo(other)
        }

        @Serializable
        data class Pause(override val packageName: String) : ControlMediaForApp() {
            override val id = ActionId.PAUSE_MEDIA_PACKAGE
        }

        @Serializable
        data class Play(override val packageName: String) : ControlMediaForApp() {
            override val id = ActionId.PLAY_MEDIA_PACKAGE
        }

        @Serializable
        data class PlayPause(override val packageName: String) : ControlMediaForApp() {
            override val id = ActionId.PLAY_PAUSE_MEDIA_PACKAGE
        }

        @Serializable
        data class NextTrack(override val packageName: String) : ControlMediaForApp() {
            override val id = ActionId.NEXT_TRACK_PACKAGE
        }

        @Serializable
        data class PreviousTrack(override val packageName: String) : ControlMediaForApp() {
            override val id = ActionId.PREVIOUS_TRACK_PACKAGE
        }

        @Serializable
        data class FastForward(override val packageName: String) : ControlMediaForApp() {
            override val id = ActionId.FAST_FORWARD_PACKAGE
        }

        @Serializable
        data class Rewind(override val packageName: String) : ControlMediaForApp() {
            override val id = ActionId.REWIND_PACKAGE
        }
    }

    @Serializable
    sealed class ControlMedia : ActionData() {
        @Serializable
        object Pause : ControlMedia() {
            override val id = ActionId.PAUSE_MEDIA
        }

        @Serializable
        object Play : ControlMedia() {
            override val id = ActionId.PLAY_MEDIA
        }

        @Serializable
        object PlayPause : ControlMedia() {
            override val id = ActionId.PLAY_PAUSE_MEDIA
        }

        @Serializable
        object NextTrack : ControlMedia() {
            override val id = ActionId.NEXT_TRACK
        }

        @Serializable
        object PreviousTrack : ControlMedia() {
            override val id = ActionId.PREVIOUS_TRACK
        }

        @Serializable
        object FastForward : ControlMedia() {
            override val id = ActionId.FAST_FORWARD
        }

        @Serializable
        object Rewind : ControlMedia() {
            override val id = ActionId.REWIND
        }
    }

    @Serializable
    data class Intent(
        val description: String,
        val target: IntentTarget,
        val uri: String,
        val extras: List<IntentExtraModel>,
    ) : ActionData() {
        override val id = ActionId.INTENT

        override fun compareTo(other: ActionData) = when (other) {
            is Intent -> description.compareTo(other.description)
            else -> super.compareTo(other)
        }
    }

    @Serializable
    data class TapScreen(
        val x: Int,
        val y: Int,
        val description: String?,
    ) : ActionData() {
        override val id = ActionId.TAP_SCREEN

        override fun compareTo(other: ActionData) = when (other) {
            is TapScreen -> compareValuesBy(
                this,
                other,
                { it.description },
                { it.x },
                { it.y },
            )

            else -> super.compareTo(other)
        }
    }

    @Serializable
    data class SwipeScreen(
        val xStart: Int,
        val yStart: Int,
        val xEnd: Int,
        val yEnd: Int,
        val fingerCount: Int,
        val duration: Int,
        val description: String?,
    ) : ActionData() {
        override val id = ActionId.SWIPE_SCREEN

        override fun compareTo(other: ActionData) = when (other) {
            is SwipeScreen -> compareValuesBy(
                this,
                other,
                { it.description },
                { it.fingerCount },
                { it.xStart },
                { it.yStart },
                { it.xEnd },
                { it.yEnd },
                { it.duration },
            )

            else -> super.compareTo(other)
        }
    }

    @Serializable
    data class PinchScreen(
        val x: Int,
        val y: Int,
        val distance: Int,
        val pinchType: PinchScreenType,
        val fingerCount: Int,
        val duration: Int,
        val description: String?,
    ) : ActionData() {
        override val id = ActionId.PINCH_SCREEN

        override fun compareTo(other: ActionData) = when (other) {
            is PinchScreen -> compareValuesBy(
                this,
                other,
                { it.description },
                { it.pinchType },
                { it.fingerCount },
                { it.x },
                { it.y },
                { it.distance },
                { it.duration },
            )

            else -> super.compareTo(other)
        }
    }

    @Serializable
    data class PhoneCall(
        val number: String,
    ) : ActionData() {
        override val id = ActionId.PHONE_CALL

        override fun compareTo(other: ActionData) = when (other) {
            is PhoneCall -> number.compareTo(other.number)
            else -> super.compareTo(other)
        }
    }

    @Serializable
    data class Url(
        val url: String,
    ) : ActionData() {
        override val id = ActionId.URL

        override fun compareTo(other: ActionData) = when (other) {
            is Url -> url.compareTo(other.url)
            else -> super.compareTo(other)
        }
    }

    @Serializable
    data class Text(
        val text: String,
    ) : ActionData() {
        override val id = ActionId.TEXT

        override fun compareTo(other: ActionData) = when (other) {
            is Text -> text.compareTo(other.text)
            else -> super.compareTo(other)
        }
    }

    @Serializable
    sealed class Wifi : ActionData() {
        @Serializable
        object Enable : Wifi() {
            override val id = ActionId.ENABLE_WIFI
        }

        @Serializable
        object Disable : Wifi() {
            override val id = ActionId.DISABLE_WIFI
        }

        @Serializable
        object Toggle : Wifi() {
            override val id = ActionId.TOGGLE_WIFI
        }
    }

    @Serializable
    sealed class Bluetooth : ActionData() {
        @Serializable
        object Enable : Bluetooth() {
            override val id = ActionId.ENABLE_BLUETOOTH
        }

        @Serializable
        object Disable : Bluetooth() {
            override val id = ActionId.DISABLE_BLUETOOTH
        }

        @Serializable
        object Toggle : Bluetooth() {
            override val id = ActionId.TOGGLE_BLUETOOTH
        }
    }

    @Serializable
    sealed class Nfc : ActionData() {
        @Serializable
        object Enable : Nfc() {
            override val id = ActionId.ENABLE_NFC
        }

        @Serializable
        object Disable : Nfc() {
            override val id = ActionId.DISABLE_NFC
        }

        @Serializable
        object Toggle : Nfc() {
            override val id = ActionId.TOGGLE_NFC
        }
    }

    @Serializable
    sealed class AirplaneMode : ActionData() {
        @Serializable
        object Enable : AirplaneMode() {
            override val id = ActionId.ENABLE_AIRPLANE_MODE
        }

        @Serializable
        object Disable : AirplaneMode() {
            override val id = ActionId.DISABLE_AIRPLANE_MODE
        }

        @Serializable
        object Toggle : AirplaneMode() {
            override val id = ActionId.TOGGLE_AIRPLANE_MODE
        }
    }

    @Serializable
    sealed class MobileData : ActionData() {
        @Serializable
        object Enable : MobileData() {
            override val id = ActionId.ENABLE_MOBILE_DATA
        }

        @Serializable
        object Disable : MobileData() {
            override val id = ActionId.DISABLE_MOBILE_DATA
        }

        @Serializable
        object Toggle : MobileData() {
            override val id = ActionId.TOGGLE_MOBILE_DATA
        }
    }

    @Serializable
    sealed class Brightness : ActionData() {
        @Serializable
        object EnableAuto : Brightness() {
            override val id = ActionId.ENABLE_AUTO_BRIGHTNESS
        }

        @Serializable
        object DisableAuto : Brightness() {
            override val id = ActionId.DISABLE_AUTO_BRIGHTNESS
        }

        @Serializable
        object ToggleAuto : Brightness() {
            override val id = ActionId.TOGGLE_AUTO_BRIGHTNESS
        }

        @Serializable
        object Increase : Brightness() {
            override val id = ActionId.INCREASE_BRIGHTNESS
        }

        @Serializable
        object Decrease : Brightness() {
            override val id = ActionId.DECREASE_BRIGHTNESS
        }
    }

    @Serializable
    sealed class StatusBar : ActionData() {
        @Serializable
        object ExpandNotifications : StatusBar() {
            override val id = ActionId.EXPAND_NOTIFICATION_DRAWER
        }

        @Serializable
        object ToggleNotifications : StatusBar() {
            override val id = ActionId.TOGGLE_NOTIFICATION_DRAWER
        }

        @Serializable
        object ExpandQuickSettings : StatusBar() {
            override val id = ActionId.EXPAND_QUICK_SETTINGS
        }

        @Serializable
        object ToggleQuickSettings : StatusBar() {
            override val id = ActionId.TOGGLE_QUICK_SETTINGS
        }

        @Serializable
        object Collapse : StatusBar() {
            override val id = ActionId.COLLAPSE_STATUS_BAR
        }
    }

    @Serializable
    object GoBack : ActionData() {
        override val id = ActionId.GO_BACK
    }

    @Serializable
    object GoHome : ActionData() {
        override val id = ActionId.GO_HOME
    }

    @Serializable
    object OpenRecents : ActionData() {
        override val id = ActionId.OPEN_RECENTS
    }

    @Serializable
    object GoLastApp : ActionData() {
        override val id = ActionId.GO_LAST_APP
    }

    @Serializable
    object OpenMenu : ActionData() {
        override val id = ActionId.OPEN_MENU
    }

    @Serializable
    object ToggleSplitScreen : ActionData() {
        override val id = ActionId.TOGGLE_SPLIT_SCREEN
    }

    @Serializable
    object Screenshot : ActionData() {
        override val id = ActionId.SCREENSHOT
    }

    @Serializable
    object MoveCursorToEnd : ActionData() {
        override val id = ActionId.MOVE_CURSOR_TO_END
    }

    @Serializable
    object ToggleKeyboard : ActionData() {
        override val id = ActionId.TOGGLE_KEYBOARD
    }

    @Serializable
    object ShowKeyboard : ActionData() {
        override val id = ActionId.SHOW_KEYBOARD
    }

    @Serializable
    object HideKeyboard : ActionData() {
        override val id = ActionId.HIDE_KEYBOARD
    }

    @Serializable
    object ShowKeyboardPicker : ActionData() {
        override val id = ActionId.SHOW_KEYBOARD_PICKER
    }

    @Serializable
    object CopyText : ActionData() {
        override val id = ActionId.TEXT_COPY
    }

    @Serializable
    object PasteText : ActionData() {
        override val id = ActionId.TEXT_PASTE
    }

    @Serializable
    object CutText : ActionData() {
        override val id = ActionId.TEXT_CUT
    }

    @Serializable
    object SelectWordAtCursor : ActionData() {
        override val id = ActionId.SELECT_WORD_AT_CURSOR
    }

    @Serializable
    object VoiceAssistant : ActionData() {
        override val id = ActionId.OPEN_VOICE_ASSISTANT
    }

    @Serializable
    object DeviceAssistant : ActionData() {
        override val id = ActionId.OPEN_DEVICE_ASSISTANT
    }

    @Serializable
    object OpenCamera : ActionData() {
        override val id = ActionId.OPEN_CAMERA
    }

    @Serializable
    object LockDevice : ActionData() {
        override val id = ActionId.LOCK_DEVICE
    }

    @Serializable
    object ScreenOnOff : ActionData() {
        override val id = ActionId.POWER_ON_OFF_DEVICE
    }

    @Serializable
    object SecureLock : ActionData() {
        override val id = ActionId.SECURE_LOCK_DEVICE
    }

    @Serializable
    object ConsumeKeyEvent : ActionData() {
        override val id = ActionId.CONSUME_KEY_EVENT
    }

    @Serializable
    object OpenSettings : ActionData() {
        override val id = ActionId.OPEN_SETTINGS
    }

    @Serializable
    object ShowPowerMenu : ActionData() {
        override val id = ActionId.SHOW_POWER_MENU
    }

    @Serializable
    object DismissLastNotification : ActionData() {
        override val id: ActionId = ActionId.DISMISS_MOST_RECENT_NOTIFICATION
    }

    @Serializable
    object DismissAllNotifications : ActionData() {
        override val id: ActionId = ActionId.DISMISS_ALL_NOTIFICATIONS
    }

    @Serializable
    object AnswerCall : ActionData() {
        override val id: ActionId = ActionId.ANSWER_PHONE_CALL
    }

    @Serializable
    object EndCall : ActionData() {
        override val id: ActionId = ActionId.END_PHONE_CALL
    }

    @Serializable
    object DeviceControls : ActionData() {
        override val id: ActionId = ActionId.DEVICE_CONTROLS
    }

    @Serializable
    data class HttpRequest(
        val description: String,
        val method: HttpMethod,
        val url: String,
        val body: String,
        val authorizationHeader: String,
    ) : ActionData() {
        override val id: ActionId = ActionId.HTTP_REQUEST

        override fun toString(): String {
            // Do not leak sensitive request info to logs.
            return "HttpRequest(description=$description)"
        }
    }
}
