package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.common.result.valueOrNull
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.util.getKey
import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.util.UUID

@Serializable
sealed class Constraint {
    abstract val uid: String
    abstract val id: ConstraintId

    @Serializable
    data class AppInForeground(
        override val uid: String = UUID.randomUUID().toString(),
        val packageName: String,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.APP_IN_FOREGROUND
    }

    @Serializable
    data class AppNotInForeground(
        override val uid: String = UUID.randomUUID().toString(),
        val packageName: String,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.APP_NOT_IN_FOREGROUND
    }

    @Serializable
    data class AppPlayingMedia(
        override val uid: String = UUID.randomUUID().toString(),
        val packageName: String,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.APP_PLAYING_MEDIA
    }

    @Serializable
    data class AppNotPlayingMedia(
        override val uid: String = UUID.randomUUID().toString(),
        val packageName: String,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.APP_NOT_PLAYING_MEDIA
    }

    @Serializable
    data class MediaPlaying(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.MEDIA_PLAYING
    }

    @Serializable
    data class NoMediaPlaying(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.MEDIA_NOT_PLAYING
    }

    @Serializable
    data class BtDeviceConnected(
        override val uid: String = UUID.randomUUID().toString(),
        val bluetoothAddress: String,
        val deviceName: String,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.BT_DEVICE_CONNECTED
    }

    @Serializable
    data class BtDeviceDisconnected(
        override val uid: String = UUID.randomUUID().toString(),
        val bluetoothAddress: String,
        val deviceName: String,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.BT_DEVICE_DISCONNECTED
    }

    @Serializable
    data class ScreenOn(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.SCREEN_ON
    }

    @Serializable
    data class ScreenOff(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.SCREEN_OFF
    }

    @Serializable
    data class OrientationPortrait(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.ORIENTATION_PORTRAIT
    }

    @Serializable
    data class OrientationLandscape(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.ORIENTATION_LANDSCAPE
    }

    @Serializable
    data class OrientationCustom(
        override val uid: String = UUID.randomUUID().toString(),
        val orientation: Orientation,
    ) : Constraint() {
        override val id: ConstraintId = when (orientation) {
            Orientation.ORIENTATION_0 -> ConstraintId.ORIENTATION_0
            Orientation.ORIENTATION_90 -> ConstraintId.ORIENTATION_90
            Orientation.ORIENTATION_180 -> ConstraintId.ORIENTATION_180
            Orientation.ORIENTATION_270 -> ConstraintId.ORIENTATION_270
        }
    }

    @Serializable
    data class FlashlightOn(
        override val uid: String = UUID.randomUUID().toString(),
        val lens: CameraLens,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.FLASHLIGHT_ON
    }

    @Serializable
    data class FlashlightOff(
        override val uid: String = UUID.randomUUID().toString(),
        val lens: CameraLens,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.FLASHLIGHT_OFF
    }

    @Serializable
    data class WifiOn(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.WIFI_ON
    }

    @Serializable
    data class WifiOff(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.WIFI_OFF
    }

    @Serializable
    data class WifiConnected(
        override val uid: String = UUID.randomUUID().toString(),
        val ssid: String?,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.WIFI_CONNECTED
    }

    @Serializable
    data class WifiDisconnected(
        override val uid: String = UUID.randomUUID().toString(),
        val ssid: String?,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.WIFI_DISCONNECTED
    }

    @Serializable
    data class ImeChosen(
        override val uid: String = UUID.randomUUID().toString(),
        val imeId: String,
        val imeLabel: String,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.IME_CHOSEN
    }

    @Serializable
    data class ImeNotChosen(
        override val uid: String = UUID.randomUUID().toString(),
        val imeId: String,
        val imeLabel: String,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.IME_NOT_CHOSEN
    }

    @Serializable
    data class DeviceIsLocked(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.DEVICE_IS_LOCKED
    }

    @Serializable
    data class DeviceIsUnlocked(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.DEVICE_IS_UNLOCKED
    }

    @Serializable
    data class LockScreenShowing(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.LOCK_SCREEN_SHOWING
    }

    @Serializable
    data class LockScreenNotShowing(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.LOCK_SCREEN_NOT_SHOWING
    }

    @Serializable
    data class InPhoneCall(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.IN_PHONE_CALL
    }

    @Serializable
    data class NotInPhoneCall(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.NOT_IN_PHONE_CALL
    }

    @Serializable
    data class PhoneRinging(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.PHONE_RINGING
    }

    @Serializable
    data class Charging(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.CHARGING
    }

    @Serializable
    data class Discharging(override val uid: String = UUID.randomUUID().toString()) : Constraint() {
        override val id: ConstraintId = ConstraintId.DISCHARGING
    }

    @Serializable
    data class Time(
        override val uid: String = UUID.randomUUID().toString(),
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
    ) : Constraint() {
        override val id: ConstraintId = ConstraintId.TIME

        val startTime: LocalTime by lazy { LocalTime.of(startHour, startMinute) }
        val endTime: LocalTime by lazy { LocalTime.of(endHour, endMinute) }
    }
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
        fun getPackageName(): String = entity.extras.getData(ConstraintEntity.EXTRA_PACKAGE_NAME).valueOrNull()!!

        fun getBluetoothAddress(): String = entity.extras.getData(ConstraintEntity.EXTRA_BT_ADDRESS).valueOrNull()!!

        fun getBluetoothDeviceName(): String = entity.extras.getData(ConstraintEntity.EXTRA_BT_NAME).valueOrNull()!!

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
            ConstraintEntity.APP_FOREGROUND -> Constraint.AppInForeground(
                uid = entity.uid,
                getPackageName(),
            )

            ConstraintEntity.APP_NOT_FOREGROUND -> Constraint.AppNotInForeground(
                uid = entity.uid,
                getPackageName(),
            )

            ConstraintEntity.APP_PLAYING_MEDIA -> Constraint.AppPlayingMedia(
                uid = entity.uid,
                getPackageName(),
            )

            ConstraintEntity.APP_NOT_PLAYING_MEDIA -> Constraint.AppNotPlayingMedia(
                uid = entity.uid,
                getPackageName(),
            )

            ConstraintEntity.MEDIA_PLAYING -> Constraint.MediaPlaying(uid = entity.uid)
            ConstraintEntity.NO_MEDIA_PLAYING -> Constraint.NoMediaPlaying(uid = entity.uid)

            ConstraintEntity.BT_DEVICE_CONNECTED ->
                Constraint.BtDeviceConnected(
                    uid = entity.uid,
                    getBluetoothAddress(),
                    getBluetoothDeviceName(),
                )

            ConstraintEntity.BT_DEVICE_DISCONNECTED ->
                Constraint.BtDeviceDisconnected(
                    uid = entity.uid,
                    getBluetoothAddress(),
                    getBluetoothDeviceName(),
                )

            ConstraintEntity.ORIENTATION_0 -> Constraint.OrientationCustom(
                uid = entity.uid,
                Orientation.ORIENTATION_0,
            )

            ConstraintEntity.ORIENTATION_90 -> Constraint.OrientationCustom(
                uid = entity.uid,
                Orientation.ORIENTATION_90,
            )

            ConstraintEntity.ORIENTATION_180 -> Constraint.OrientationCustom(
                uid = entity.uid,
                Orientation.ORIENTATION_180,
            )

            ConstraintEntity.ORIENTATION_270 -> Constraint.OrientationCustom(
                uid = entity.uid,
                Orientation.ORIENTATION_270,
            )

            ConstraintEntity.ORIENTATION_PORTRAIT -> Constraint.OrientationPortrait(uid = entity.uid)
            ConstraintEntity.ORIENTATION_LANDSCAPE -> Constraint.OrientationLandscape(uid = entity.uid)

            ConstraintEntity.SCREEN_OFF -> Constraint.ScreenOff(uid = entity.uid)
            ConstraintEntity.SCREEN_ON -> Constraint.ScreenOn(uid = entity.uid)

            ConstraintEntity.FLASHLIGHT_ON -> Constraint.FlashlightOn(
                uid = entity.uid,
                getCameraLens(),
            )

            ConstraintEntity.FLASHLIGHT_OFF -> Constraint.FlashlightOff(
                uid = entity.uid,
                getCameraLens(),
            )

            ConstraintEntity.WIFI_ON -> Constraint.WifiOn(uid = entity.uid)
            ConstraintEntity.WIFI_OFF -> Constraint.WifiOff(uid = entity.uid)
            ConstraintEntity.WIFI_CONNECTED -> Constraint.WifiConnected(uid = entity.uid, getSsid())
            ConstraintEntity.WIFI_DISCONNECTED -> Constraint.WifiDisconnected(
                uid = entity.uid,
                getSsid(),
            )

            ConstraintEntity.IME_CHOSEN -> Constraint.ImeChosen(
                uid = entity.uid,
                getImeId(),
                getImeLabel(),
            )

            ConstraintEntity.IME_NOT_CHOSEN -> Constraint.ImeNotChosen(
                uid = entity.uid,
                getImeId(),
                getImeLabel(),
            )

            ConstraintEntity.DEVICE_IS_UNLOCKED -> Constraint.DeviceIsUnlocked(uid = entity.uid)
            ConstraintEntity.DEVICE_IS_LOCKED -> Constraint.DeviceIsLocked(uid = entity.uid)
            ConstraintEntity.LOCK_SCREEN_SHOWING -> Constraint.LockScreenShowing(uid = entity.uid)
            ConstraintEntity.LOCK_SCREEN_NOT_SHOWING -> Constraint.LockScreenNotShowing(uid = entity.uid)

            ConstraintEntity.PHONE_RINGING -> Constraint.PhoneRinging(uid = entity.uid)
            ConstraintEntity.IN_PHONE_CALL -> Constraint.InPhoneCall(uid = entity.uid)
            ConstraintEntity.NOT_IN_PHONE_CALL -> Constraint.NotInPhoneCall(uid = entity.uid)

            ConstraintEntity.CHARGING -> Constraint.Charging(uid = entity.uid)
            ConstraintEntity.DISCHARGING -> Constraint.Discharging(uid = entity.uid)

            ConstraintEntity.TIME -> {
                val startTime =
                    entity.extras.getData(ConstraintEntity.EXTRA_START_TIME).valueOrNull()!!
                        .split(":")
                val startHour = startTime[0].toInt()
                val startMin = startTime[1].toInt()

                val endTime =
                    entity.extras.getData(ConstraintEntity.EXTRA_END_TIME).valueOrNull()!!
                        .split(":")
                val endHour = endTime[0].toInt()
                val endMin = endTime[1].toInt()

                Constraint.Time(
                    uid = entity.uid,
                    startHour = startHour,
                    startMinute = startMin,
                    endHour = endHour,
                    endMinute = endMin,
                )
            }

            else -> throw Exception("don't know how to convert constraint entity with type ${entity.type}")
        }
    }

    fun toEntity(constraint: Constraint): ConstraintEntity = when (constraint) {
        is Constraint.AppInForeground -> ConstraintEntity(
            uid = constraint.uid,
            type = ConstraintEntity.APP_FOREGROUND,
            extras = listOf(
                EntityExtra(
                    ConstraintEntity.EXTRA_PACKAGE_NAME,
                    constraint.packageName,
                ),
            ),
        )

        is Constraint.AppNotInForeground -> ConstraintEntity(
            uid = constraint.uid,
            type = ConstraintEntity.APP_NOT_FOREGROUND,
            extras = listOf(
                EntityExtra(
                    ConstraintEntity.EXTRA_PACKAGE_NAME,
                    constraint.packageName,
                ),
            ),
        )

        is Constraint.AppPlayingMedia -> ConstraintEntity(
            uid = constraint.uid,
            type = ConstraintEntity.APP_PLAYING_MEDIA,
            extras = listOf(
                EntityExtra(
                    ConstraintEntity.EXTRA_PACKAGE_NAME,
                    constraint.packageName,
                ),
            ),
        )

        is Constraint.AppNotPlayingMedia -> ConstraintEntity(
            uid = constraint.uid,
            type = ConstraintEntity.APP_NOT_PLAYING_MEDIA,
            extras = listOf(
                EntityExtra(
                    ConstraintEntity.EXTRA_PACKAGE_NAME,
                    constraint.packageName,
                ),
            ),
        )

        is Constraint.MediaPlaying -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.MEDIA_PLAYING,
        )

        is Constraint.NoMediaPlaying -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.NO_MEDIA_PLAYING,
        )

        is Constraint.BtDeviceConnected -> ConstraintEntity(
            uid = constraint.uid,
            type = ConstraintEntity.BT_DEVICE_CONNECTED,
            extras = listOf(
                EntityExtra(ConstraintEntity.EXTRA_BT_ADDRESS, constraint.bluetoothAddress),
                EntityExtra(ConstraintEntity.EXTRA_BT_NAME, constraint.deviceName),
            ),
        )

        is Constraint.BtDeviceDisconnected -> ConstraintEntity(
            uid = constraint.uid,
            type = ConstraintEntity.BT_DEVICE_DISCONNECTED,
            extras = listOf(
                EntityExtra(ConstraintEntity.EXTRA_BT_ADDRESS, constraint.bluetoothAddress),
                EntityExtra(ConstraintEntity.EXTRA_BT_NAME, constraint.deviceName),
            ),
        )

        is Constraint.OrientationCustom -> when (constraint.orientation) {
            Orientation.ORIENTATION_0 -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.ORIENTATION_0,
            )

            Orientation.ORIENTATION_90 -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.ORIENTATION_90,
            )

            Orientation.ORIENTATION_180 -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.ORIENTATION_180,
            )

            Orientation.ORIENTATION_270 -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.ORIENTATION_270,
            )
        }

        is Constraint.OrientationLandscape -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.ORIENTATION_LANDSCAPE,
        )

        is Constraint.OrientationPortrait -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.ORIENTATION_PORTRAIT,
        )

        is Constraint.ScreenOff -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.SCREEN_OFF,
        )

        is Constraint.ScreenOn -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.SCREEN_ON,
        )

        is Constraint.FlashlightOff -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.FLASHLIGHT_OFF,
            EntityExtra(ConstraintEntity.EXTRA_FLASHLIGHT_CAMERA_LENS, LENS_MAP[constraint.lens]!!),
        )

        is Constraint.FlashlightOn -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.FLASHLIGHT_ON,
            EntityExtra(ConstraintEntity.EXTRA_FLASHLIGHT_CAMERA_LENS, LENS_MAP[constraint.lens]!!),
        )

        is Constraint.WifiConnected -> {
            val extras = mutableListOf<EntityExtra>()

            if (constraint.ssid != null) {
                extras.add(EntityExtra(ConstraintEntity.EXTRA_SSID, constraint.ssid))
            }

            ConstraintEntity(
                uid = constraint.uid,
                type = ConstraintEntity.WIFI_CONNECTED,
                extras = extras,
            )
        }

        is Constraint.WifiDisconnected -> {
            val extras = mutableListOf<EntityExtra>()

            if (constraint.ssid != null) {
                extras.add(EntityExtra(ConstraintEntity.EXTRA_SSID, constraint.ssid))
            }

            ConstraintEntity(
                uid = constraint.uid,
                type = ConstraintEntity.WIFI_DISCONNECTED,
                extras = extras,
            )
        }

        is Constraint.WifiOff -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.WIFI_OFF,
        )

        is Constraint.WifiOn -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.WIFI_ON,
        )

        is Constraint.ImeChosen -> {
            ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.IME_CHOSEN,
                EntityExtra(ConstraintEntity.EXTRA_IME_ID, constraint.imeId),
                EntityExtra(ConstraintEntity.EXTRA_IME_LABEL, constraint.imeLabel),
            )
        }

        is Constraint.ImeNotChosen -> {
            ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.IME_NOT_CHOSEN,
                EntityExtra(ConstraintEntity.EXTRA_IME_ID, constraint.imeId),
                EntityExtra(ConstraintEntity.EXTRA_IME_LABEL, constraint.imeLabel),
            )
        }

        is Constraint.DeviceIsLocked -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.DEVICE_IS_LOCKED,
        )

        is Constraint.DeviceIsUnlocked -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.DEVICE_IS_UNLOCKED,
        )

        is Constraint.LockScreenShowing -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.LOCK_SCREEN_SHOWING,
        )

        is Constraint.LockScreenNotShowing -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.LOCK_SCREEN_NOT_SHOWING,
        )

        is Constraint.InPhoneCall -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.IN_PHONE_CALL,
        )

        is Constraint.NotInPhoneCall -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.NOT_IN_PHONE_CALL,
        )

        is Constraint.PhoneRinging -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.PHONE_RINGING,
        )

        is Constraint.Charging -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.CHARGING,
        )

        is Constraint.Discharging -> ConstraintEntity(
            uid = constraint.uid,
            ConstraintEntity.DISCHARGING,
        )

        is Constraint.Time -> ConstraintEntity(
            uid = constraint.uid,
            type = ConstraintEntity.TIME,
            EntityExtra(
                ConstraintEntity.EXTRA_START_TIME,
                "${constraint.startHour}:${constraint.startMinute}",
            ),
            EntityExtra(
                ConstraintEntity.EXTRA_END_TIME,
                "${constraint.endHour}:${constraint.endMinute}",
            ),
        )
    }
}
