package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.util.getKey
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Created by sds100 on 03/03/2021.
 */

@Serializable
sealed class Constraint : Comparable<Constraint> {
    val uid: String = UUID.randomUUID().toString()

    override fun compareTo(other: Constraint) = this.javaClass.name.compareTo(other.javaClass.name)

    sealed class AppConstraint : Constraint() {
        abstract val packageName: String
        override fun compareTo(other: Constraint): Int {
            if (other !is AppConstraint) {
                return super<Constraint>.compareTo(other)
            }

            return this.packageName.compareTo(other.packageName)
        }
    }

    @Serializable
    data class AppInForeground(override val packageName: String) : AppConstraint()

    @Serializable
    data class AppNotInForeground(override val packageName: String) : AppConstraint()

    @Serializable
    data class AppPlayingMedia(override val packageName: String) : AppConstraint()

    @Serializable
    data class AppNotPlayingMedia(override val packageName: String) : AppConstraint()

    @Serializable
    object MediaPlaying : Constraint()

    @Serializable
    object NoMediaPlaying : Constraint()

    sealed class BtConstraint : Constraint() {
        abstract val bluetoothAddress: String
        abstract val deviceName: String
        override fun compareTo(other: Constraint): Int {
            if (other !is BtConstraint) {
                return super<Constraint>.compareTo(other)
            }

            val address = this.bluetoothAddress.compareTo(other.bluetoothAddress)

            if (address != 0) {
                return address
            }

            return this.deviceName.compareTo(other.deviceName)
        }
    }

    @Serializable
    data class BtDeviceConnected(
        override val bluetoothAddress: String,
        override val deviceName: String,
    ) : BtConstraint()

    @Serializable
    data class BtDeviceDisconnected(
        override val bluetoothAddress: String,
        override val deviceName: String,
    ) : BtConstraint()

    @Serializable
    object ScreenOn : Constraint()

    @Serializable
    object ScreenOff : Constraint()

    @Serializable
    object OrientationPortrait : Constraint()

    @Serializable
    object OrientationLandscape : Constraint()

    @Serializable
    data class OrientationCustom(val orientation: Orientation) : Constraint() {
        override fun compareTo(other: Constraint): Int {
            if (other !is OrientationCustom) {
                return super<Constraint>.compareTo(other)
            }

            return this.orientation.compareTo(other.orientation)
        }
    }

    sealed class FlashlightConstraint : Constraint() {
        abstract val lens: CameraLens
        override fun compareTo(other: Constraint): Int {
            if (other !is FlashlightConstraint) {
                return super<Constraint>.compareTo(other)
            }

            return this.lens.compareTo(other.lens)
        }
    }

    @Serializable
    data class FlashlightOn(override val lens: CameraLens) : FlashlightConstraint()

    @Serializable
    data class FlashlightOff(override val lens: CameraLens) : FlashlightConstraint()

    @Serializable
    object WifiOn : Constraint()

    @Serializable
    object WifiOff : Constraint()

    sealed class WifiConstraint : Constraint() {
        abstract val ssid: String?
        override fun compareTo(other: Constraint): Int {
            if (other !is WifiConstraint) {
                return super<Constraint>.compareTo(other)
            }

            return this.ssid?.compareTo(other.ssid ?: "") ?: 0
        }
    }

    @Serializable
    data class WifiConnected(
        /**
         * Null if connected to any wifi network.
         */
        override val ssid: String?,
    ) : WifiConstraint()

    @Serializable
    data class WifiDisconnected(
        /**
         * Null if disconnected from any wifi network.
         */
        override val ssid: String?,
    ) : WifiConstraint()

    sealed class ImeConstraint : Constraint() {
        abstract val imeId: String
        abstract val imeLabel: String
        override fun compareTo(other: Constraint): Int {
            if (other !is ImeConstraint) {
                return super<Constraint>.compareTo(other)
            }

            val imeId = this.imeId.compareTo(other.imeId)

            if (imeId != 0) {
                return imeId
            }

            return this.imeLabel.compareTo(other.imeLabel)
        }
    }

    @Serializable
    data class ImeChosen(
        override val imeId: String,
        override val imeLabel: String,
    ) : ImeConstraint()

    @Serializable
    data class ImeNotChosen(
        override val imeId: String,
        override val imeLabel: String,
    ) : ImeConstraint()

    @Serializable
    object DeviceIsLocked : Constraint()

    @Serializable
    object DeviceIsUnlocked : Constraint()

    @Serializable
    object InPhoneCall : Constraint()

    @Serializable
    object NotInPhoneCall : Constraint()

    @Serializable
    object PhoneRinging : Constraint()

    @Serializable
    object Charging : Constraint()

    @Serializable
    object Discharging : Constraint()
}

object ConstraintModeEntityMapper {
    fun fromEntity(entity: Int): ConstraintMode = when (entity) {
        ConstraintEntity.MODE_AND -> ConstraintMode.AND
        ConstraintEntity.MODE_OR -> ConstraintMode.OR
        else -> throw Exception("don't know how to convert constraint mode entity $entity")
    }

    fun toEntity(constraintMode: ConstraintMode): Int = when (constraintMode) {
        ConstraintMode.AND -> ConstraintEntity.MODE_AND
        ConstraintMode.OR -> ConstraintEntity.MODE_OR
    }
}

object ConstraintEntityMapper {

    private val LENS_MAP = mapOf(
        CameraLens.BACK to "option_lens_back",
        CameraLens.FRONT to "option_lens_front",
    )

    fun fromEntity(entity: ConstraintEntity): Constraint {
        fun getPackageName(): String =
            entity.extras.getData(ConstraintEntity.EXTRA_PACKAGE_NAME).valueOrNull()!!

        fun getBluetoothAddress(): String =
            entity.extras.getData(ConstraintEntity.EXTRA_BT_ADDRESS).valueOrNull()!!

        fun getBluetoothDeviceName(): String =
            entity.extras.getData(ConstraintEntity.EXTRA_BT_NAME).valueOrNull()!!

        fun getCameraLens(): CameraLens {
            val extraValue =
                entity.extras.getData(ConstraintEntity.EXTRA_FLASHLIGHT_CAMERA_LENS).valueOrNull()!!
            return LENS_MAP.getKey(extraValue)!!
        }

        fun getSsid(): String? {
            val extraValue =
                entity.extras.getData(ConstraintEntity.EXTRA_SSID).valueOrNull()
            return extraValue
        }

        fun getImeId(): String {
            val extraValue =
                entity.extras.getData(ConstraintEntity.EXTRA_IME_ID).valueOrNull()!!
            return extraValue
        }

        fun getImeLabel(): String {
            val extraValue =
                entity.extras.getData(ConstraintEntity.EXTRA_IME_LABEL).valueOrNull()!!
            return extraValue
        }

        return when (entity.type) {
            ConstraintEntity.APP_FOREGROUND -> Constraint.AppInForeground(getPackageName())
            ConstraintEntity.APP_NOT_FOREGROUND -> Constraint.AppNotInForeground(getPackageName())
            ConstraintEntity.APP_PLAYING_MEDIA -> Constraint.AppPlayingMedia(getPackageName())
            ConstraintEntity.APP_NOT_PLAYING_MEDIA -> Constraint.AppNotPlayingMedia(getPackageName())
            ConstraintEntity.MEDIA_PLAYING -> Constraint.MediaPlaying
            ConstraintEntity.NO_MEDIA_PLAYING -> Constraint.NoMediaPlaying

            ConstraintEntity.BT_DEVICE_CONNECTED ->
                Constraint.BtDeviceConnected(getBluetoothAddress(), getBluetoothDeviceName())

            ConstraintEntity.BT_DEVICE_DISCONNECTED ->
                Constraint.BtDeviceDisconnected(getBluetoothAddress(), getBluetoothDeviceName())

            ConstraintEntity.ORIENTATION_0 -> Constraint.OrientationCustom(Orientation.ORIENTATION_0)
            ConstraintEntity.ORIENTATION_90 -> Constraint.OrientationCustom(Orientation.ORIENTATION_90)
            ConstraintEntity.ORIENTATION_180 -> Constraint.OrientationCustom(Orientation.ORIENTATION_180)
            ConstraintEntity.ORIENTATION_270 -> Constraint.OrientationCustom(Orientation.ORIENTATION_270)

            ConstraintEntity.ORIENTATION_PORTRAIT -> Constraint.OrientationPortrait
            ConstraintEntity.ORIENTATION_LANDSCAPE -> Constraint.OrientationLandscape

            ConstraintEntity.SCREEN_OFF -> Constraint.ScreenOff
            ConstraintEntity.SCREEN_ON -> Constraint.ScreenOn

            ConstraintEntity.FLASHLIGHT_ON -> Constraint.FlashlightOn(getCameraLens())
            ConstraintEntity.FLASHLIGHT_OFF -> Constraint.FlashlightOff(getCameraLens())

            ConstraintEntity.WIFI_ON -> Constraint.WifiOn
            ConstraintEntity.WIFI_OFF -> Constraint.WifiOff
            ConstraintEntity.WIFI_CONNECTED -> Constraint.WifiConnected(getSsid())
            ConstraintEntity.WIFI_DISCONNECTED -> Constraint.WifiDisconnected(getSsid())

            ConstraintEntity.IME_CHOSEN -> Constraint.ImeChosen(getImeId(), getImeLabel())
            ConstraintEntity.IME_NOT_CHOSEN -> Constraint.ImeNotChosen(getImeId(), getImeLabel())

            ConstraintEntity.DEVICE_IS_UNLOCKED -> Constraint.DeviceIsUnlocked
            ConstraintEntity.DEVICE_IS_LOCKED -> Constraint.DeviceIsLocked

            ConstraintEntity.PHONE_RINGING -> Constraint.PhoneRinging
            ConstraintEntity.IN_PHONE_CALL -> Constraint.InPhoneCall
            ConstraintEntity.NOT_IN_PHONE_CALL -> Constraint.NotInPhoneCall

            ConstraintEntity.CHARGING -> Constraint.Charging
            ConstraintEntity.DISCHARGING -> Constraint.Discharging

            else -> throw Exception("don't know how to convert constraint entity with type ${entity.type}")
        }
    }

    fun toEntity(constraint: Constraint): ConstraintEntity = when (constraint) {
        is Constraint.AppInForeground -> ConstraintEntity(
            type = ConstraintEntity.APP_FOREGROUND,
            extras = listOf(Extra(ConstraintEntity.EXTRA_PACKAGE_NAME, constraint.packageName)),
        )

        is Constraint.AppNotInForeground -> ConstraintEntity(
            type = ConstraintEntity.APP_NOT_FOREGROUND,
            extras = listOf(Extra(ConstraintEntity.EXTRA_PACKAGE_NAME, constraint.packageName)),
        )

        is Constraint.AppPlayingMedia -> ConstraintEntity(
            type = ConstraintEntity.APP_PLAYING_MEDIA,
            extras = listOf(Extra(ConstraintEntity.EXTRA_PACKAGE_NAME, constraint.packageName)),
        )

        is Constraint.AppNotPlayingMedia -> ConstraintEntity(
            type = ConstraintEntity.APP_NOT_PLAYING_MEDIA,
            extras = listOf(Extra(ConstraintEntity.EXTRA_PACKAGE_NAME, constraint.packageName)),
        )

        Constraint.MediaPlaying -> ConstraintEntity(ConstraintEntity.MEDIA_PLAYING)
        Constraint.NoMediaPlaying -> ConstraintEntity(ConstraintEntity.NO_MEDIA_PLAYING)

        is Constraint.BtDeviceConnected -> ConstraintEntity(
            type = ConstraintEntity.BT_DEVICE_CONNECTED,
            extras = listOf(
                Extra(ConstraintEntity.EXTRA_BT_ADDRESS, constraint.bluetoothAddress),
                Extra(ConstraintEntity.EXTRA_BT_NAME, constraint.deviceName),
            ),
        )

        is Constraint.BtDeviceDisconnected -> ConstraintEntity(
            type = ConstraintEntity.BT_DEVICE_DISCONNECTED,
            extras = listOf(
                Extra(ConstraintEntity.EXTRA_BT_ADDRESS, constraint.bluetoothAddress),
                Extra(ConstraintEntity.EXTRA_BT_NAME, constraint.deviceName),
            ),
        )

        is Constraint.OrientationCustom -> when (constraint.orientation) {
            Orientation.ORIENTATION_0 -> ConstraintEntity(ConstraintEntity.ORIENTATION_0)
            Orientation.ORIENTATION_90 -> ConstraintEntity(ConstraintEntity.ORIENTATION_90)
            Orientation.ORIENTATION_180 -> ConstraintEntity(ConstraintEntity.ORIENTATION_180)
            Orientation.ORIENTATION_270 -> ConstraintEntity(ConstraintEntity.ORIENTATION_270)
        }

        Constraint.OrientationLandscape -> ConstraintEntity(ConstraintEntity.ORIENTATION_LANDSCAPE)
        Constraint.OrientationPortrait -> ConstraintEntity(ConstraintEntity.ORIENTATION_PORTRAIT)
        Constraint.ScreenOff -> ConstraintEntity(ConstraintEntity.SCREEN_OFF)
        Constraint.ScreenOn -> ConstraintEntity(ConstraintEntity.SCREEN_ON)

        is Constraint.FlashlightOff -> ConstraintEntity(
            ConstraintEntity.FLASHLIGHT_OFF,
            Extra(ConstraintEntity.EXTRA_FLASHLIGHT_CAMERA_LENS, LENS_MAP[constraint.lens]!!),
        )

        is Constraint.FlashlightOn -> ConstraintEntity(
            ConstraintEntity.FLASHLIGHT_ON,
            Extra(ConstraintEntity.EXTRA_FLASHLIGHT_CAMERA_LENS, LENS_MAP[constraint.lens]!!),
        )

        is Constraint.WifiConnected -> {
            val extras = mutableListOf<Extra>()

            if (constraint.ssid != null) {
                extras.add(Extra(ConstraintEntity.EXTRA_SSID, constraint.ssid))
            }

            ConstraintEntity(ConstraintEntity.WIFI_CONNECTED, extras)
        }

        is Constraint.WifiDisconnected -> {
            val extras = mutableListOf<Extra>()

            if (constraint.ssid != null) {
                extras.add(Extra(ConstraintEntity.EXTRA_SSID, constraint.ssid))
            }

            ConstraintEntity(ConstraintEntity.WIFI_DISCONNECTED, extras)
        }

        Constraint.WifiOff -> ConstraintEntity(ConstraintEntity.WIFI_OFF)
        Constraint.WifiOn -> ConstraintEntity(ConstraintEntity.WIFI_ON)

        is Constraint.ImeChosen -> {
            ConstraintEntity(
                ConstraintEntity.IME_CHOSEN,
                Extra(ConstraintEntity.EXTRA_IME_ID, constraint.imeId),
                Extra(ConstraintEntity.EXTRA_IME_LABEL, constraint.imeLabel),
            )
        }

        is Constraint.ImeNotChosen -> {
            ConstraintEntity(
                ConstraintEntity.IME_NOT_CHOSEN,
                Extra(ConstraintEntity.EXTRA_IME_ID, constraint.imeId),
                Extra(ConstraintEntity.EXTRA_IME_LABEL, constraint.imeLabel),
            )
        }

        Constraint.DeviceIsLocked -> ConstraintEntity(ConstraintEntity.DEVICE_IS_LOCKED)
        Constraint.DeviceIsUnlocked -> ConstraintEntity(ConstraintEntity.DEVICE_IS_UNLOCKED)
        Constraint.InPhoneCall -> ConstraintEntity(ConstraintEntity.IN_PHONE_CALL)
        Constraint.NotInPhoneCall -> ConstraintEntity(ConstraintEntity.NOT_IN_PHONE_CALL)
        Constraint.PhoneRinging -> ConstraintEntity(ConstraintEntity.PHONE_RINGING)
        Constraint.Charging -> ConstraintEntity(ConstraintEntity.CHARGING)
        Constraint.Discharging -> ConstraintEntity(ConstraintEntity.DISCHARGING)
    }
}
