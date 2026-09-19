package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.volume.RingerMode as SystemRingerMode
import java.time.LocalTime
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
sealed class ConstraintData {
    abstract val id: ConstraintId

    @Serializable
    data class AppInForeground(val packageName: String) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.APP_IN_FOREGROUND
    }

    @Serializable
    data class AppPlayingMedia(val packageName: String) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.APP_PLAYING_MEDIA
    }

    @Serializable
    data object MediaPlaying : ConstraintData() {
        override val id: ConstraintId = ConstraintId.MEDIA_PLAYING
    }

    @Serializable
    data class BtDeviceConnected(val bluetoothAddress: String, val deviceName: String) :
        ConstraintData() {
        override val id: ConstraintId = ConstraintId.BT_DEVICE_CONNECTED
    }

    @Serializable
    data object ScreenOn : ConstraintData() {
        override val id: ConstraintId = ConstraintId.SCREEN_ON
    }

    @Serializable
    data object OrientationPortrait : ConstraintData() {
        override val id: ConstraintId = ConstraintId.DISPLAY_ORIENTATION_PORTRAIT
    }

    @Serializable
    data object OrientationLandscape : ConstraintData() {
        override val id: ConstraintId = ConstraintId.DISPLAY_ORIENTATION_LANDSCAPE
    }

    @Serializable
    data class OrientationCustom(val orientation: Orientation) : ConstraintData() {
        override val id: ConstraintId = when (orientation) {
            Orientation.ORIENTATION_0 -> ConstraintId.DISPLAY_ORIENTATION_0
            Orientation.ORIENTATION_90 -> ConstraintId.DISPLAY_ORIENTATION_90
            Orientation.ORIENTATION_180 -> ConstraintId.DISPLAY_ORIENTATION_180
            Orientation.ORIENTATION_270 -> ConstraintId.DISPLAY_ORIENTATION_270
        }
    }

    @Serializable
    data class PhysicalOrientation(
        val physicalOrientation: io.github.sds100.keymapper.common.utils.PhysicalOrientation,
    ) : ConstraintData() {
        override val id: ConstraintId = when (physicalOrientation) {
            io.github.sds100.keymapper.common.utils.PhysicalOrientation.PORTRAIT ->
                ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT

            io.github.sds100.keymapper.common.utils.PhysicalOrientation.LANDSCAPE ->
                ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE

            io.github.sds100.keymapper.common.utils.PhysicalOrientation.PORTRAIT_INVERTED ->
                ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT_INVERTED

            io.github.sds100.keymapper.common.utils.PhysicalOrientation.LANDSCAPE_INVERTED ->
                ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE_INVERTED
        }
    }

    @Serializable
    data class DisplayResolution(val width: Int, val height: Int) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.DISPLAY_RESOLUTION
    }

    @Serializable
    data class FlashlightOn(val lens: CameraLens) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.FLASHLIGHT_ON
    }

    @Serializable
    data object WifiOn : ConstraintData() {
        override val id: ConstraintId = ConstraintId.WIFI_ON
    }

    @Serializable
    data class WifiConnected(val ssid: String?) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.WIFI_CONNECTED
    }

    @Serializable
    data class ImeChosen(val imeId: String, val imeLabel: String) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.IME_CHOSEN
    }

    @Serializable
    data object KeyboardShowing : ConstraintData() {
        override val id: ConstraintId = ConstraintId.KEYBOARD_SHOWING
    }

    @Serializable
    data object DeviceIsLocked : ConstraintData() {
        override val id: ConstraintId = ConstraintId.DEVICE_IS_LOCKED
    }

    @Serializable
    data object LockScreenShowing : ConstraintData() {
        override val id: ConstraintId = ConstraintId.LOCK_SCREEN_SHOWING
    }

    @Serializable
    data object InPhoneCall : ConstraintData() {
        override val id: ConstraintId = ConstraintId.IN_PHONE_CALL
    }

    @Serializable
    data object NotInPhoneCall : ConstraintData() {
        override val id: ConstraintId = ConstraintId.NOT_IN_PHONE_CALL
    }

    @Serializable
    data object PhoneRinging : ConstraintData() {
        override val id: ConstraintId = ConstraintId.PHONE_RINGING
    }

    @Serializable
    data class RingerMode(val ringerMode: SystemRingerMode) : ConstraintData() {
        override val id: ConstraintId = when (ringerMode) {
            SystemRingerMode.NORMAL -> ConstraintId.RINGER_MODE_NORMAL
            SystemRingerMode.VIBRATE -> ConstraintId.RINGER_MODE_VIBRATE
            SystemRingerMode.SILENT -> ConstraintId.RINGER_MODE_SILENT
        }
    }

    @Serializable
    data object Charging : ConstraintData() {
        override val id: ConstraintId = ConstraintId.CHARGING
    }

    @Serializable
    data object HingeClosed : ConstraintData() {
        override val id: ConstraintId = ConstraintId.HINGE_CLOSED
    }

    @Serializable
    data object HingeOpen : ConstraintData() {
        override val id: ConstraintId = ConstraintId.HINGE_OPEN
    }

    @Serializable
    data object NotificationPanelShowing : ConstraintData() {
        override val id: ConstraintId = ConstraintId.NOTIFICATION_PANEL_SHOWING
    }

    @Serializable
    sealed class NotificationPosted : ConstraintData() {
        override val id: ConstraintId get() = ConstraintId.NOTIFICATION_POSTED

        /**
         * A variant that compares free text the user typed, so it can be matched loosely. Matching
         * is always case insensitive
         */
        @Serializable
        sealed class TextMatch : NotificationPosted() {
            abstract val text: String
            abstract val matchMode: TextMatchMode
        }

        @Serializable
        data class Title(override val text: String, override val matchMode: TextMatchMode) :
            TextMatch()

        @Serializable
        data class Text(override val text: String, override val matchMode: TextMatchMode) :
            TextMatch()

        @Serializable
        data class FromApp(val packageName: String) : NotificationPosted()
    }

    @Serializable
    data class Time(
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.TIME

        val startTime: LocalTime by lazy { LocalTime.of(startHour, startMinute) }
        val endTime: LocalTime by lazy { LocalTime.of(endHour, endMinute) }
    }
}

@Serializable
data class Constraint(
    val uid: String = UUID.randomUUID().toString(),
    val data: ConstraintData,
    /**
     * Whether the constraint is inverted so it is satisfied when the [data] is not.
     */
    val isNot: Boolean = false,
) {
    val id: ConstraintId get() = data.id
}
