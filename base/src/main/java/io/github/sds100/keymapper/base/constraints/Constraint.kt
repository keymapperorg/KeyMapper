package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.getKey
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.system.camera.CameraLens
import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.util.UUID

@Serializable
sealed class ConstraintData {
    abstract val id: ConstraintId

    @Serializable
    data class AppInForeground(
        val packageName: String,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.APP_IN_FOREGROUND
    }

    @Serializable
    data class AppNotInForeground(
        val packageName: String,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.APP_NOT_IN_FOREGROUND
    }

    @Serializable
    data class AppPlayingMedia(
        val packageName: String,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.APP_PLAYING_MEDIA
    }

    @Serializable
    data class AppNotPlayingMedia(
        val packageName: String,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.APP_NOT_PLAYING_MEDIA
    }

    @Serializable
    data object MediaPlaying : ConstraintData() {
        override val id: ConstraintId = ConstraintId.MEDIA_PLAYING
    }

    @Serializable
    data object NoMediaPlaying : ConstraintData() {
        override val id: ConstraintId = ConstraintId.MEDIA_NOT_PLAYING
    }

    @Serializable
    data class BtDeviceConnected(
        val bluetoothAddress: String,
        val deviceName: String,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.BT_DEVICE_CONNECTED
    }

    @Serializable
    data class BtDeviceDisconnected(
        val bluetoothAddress: String,
        val deviceName: String,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.BT_DEVICE_DISCONNECTED
    }

    @Serializable
    data object ScreenOn : ConstraintData() {
        override val id: ConstraintId = ConstraintId.SCREEN_ON
    }

    @Serializable
    data object ScreenOff : ConstraintData() {
        override val id: ConstraintId = ConstraintId.SCREEN_OFF
    }

    @Serializable
    data object OrientationPortrait : ConstraintData() {
        override val id: ConstraintId = ConstraintId.ORIENTATION_PORTRAIT
    }

    @Serializable
    data object OrientationLandscape : ConstraintData() {
        override val id: ConstraintId = ConstraintId.ORIENTATION_LANDSCAPE
    }

    @Serializable
    data class OrientationCustom(
        val orientation: Orientation,
    ) : ConstraintData() {
        override val id: ConstraintId =
            when (orientation) {
                Orientation.ORIENTATION_0 -> ConstraintId.ORIENTATION_0
                Orientation.ORIENTATION_90 -> ConstraintId.ORIENTATION_90
                Orientation.ORIENTATION_180 -> ConstraintId.ORIENTATION_180
                Orientation.ORIENTATION_270 -> ConstraintId.ORIENTATION_270
            }
    }

    @Serializable
    data class FlashlightOn(
        val lens: CameraLens,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.FLASHLIGHT_ON
    }

    @Serializable
    data class FlashlightOff(
        val lens: CameraLens,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.FLASHLIGHT_OFF
    }

    @Serializable
    data object WifiOn : ConstraintData() {
        override val id: ConstraintId = ConstraintId.WIFI_ON
    }

    @Serializable
    data object WifiOff : ConstraintData() {
        override val id: ConstraintId = ConstraintId.WIFI_OFF
    }

    @Serializable
    data class WifiConnected(
        val ssid: String?,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.WIFI_CONNECTED
    }

    @Serializable
    data class WifiDisconnected(
        val ssid: String?,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.WIFI_DISCONNECTED
    }

    @Serializable
    data class ImeChosen(
        val imeId: String,
        val imeLabel: String,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.IME_CHOSEN
    }

    @Serializable
    data class ImeNotChosen(
        val imeId: String,
        val imeLabel: String,
    ) : ConstraintData() {
        override val id: ConstraintId = ConstraintId.IME_NOT_CHOSEN
    }

    @Serializable
    data object DeviceIsLocked : ConstraintData() {
        override val id: ConstraintId = ConstraintId.DEVICE_IS_LOCKED
    }

    @Serializable
    data object DeviceIsUnlocked : ConstraintData() {
        override val id: ConstraintId = ConstraintId.DEVICE_IS_UNLOCKED
    }

    @Serializable
    data object LockScreenShowing : ConstraintData() {
        override val id: ConstraintId = ConstraintId.LOCK_SCREEN_SHOWING
    }

    @Serializable
    data object LockScreenNotShowing : ConstraintData() {
        override val id: ConstraintId = ConstraintId.LOCK_SCREEN_NOT_SHOWING
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
    data object Charging : ConstraintData() {
        override val id: ConstraintId = ConstraintId.CHARGING
    }

    @Serializable
    data object Discharging : ConstraintData() {
        override val id: ConstraintId = ConstraintId.DISCHARGING
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
) {
    val id: ConstraintId get() = data.id
}

object ConstraintModeEntityMapper {
    fun fromEntity(entity: Int): ConstraintMode =
        when (entity) {
            ConstraintEntity.MODE_AND -> ConstraintMode.AND
            ConstraintEntity.MODE_OR -> ConstraintMode.OR
            else -> throw Exception("don't know how to convert constraint mode entity $entity")
        }

    fun toEntity(constraintMode: ConstraintMode): Int =
        when (constraintMode) {
            ConstraintMode.AND -> ConstraintEntity.MODE_AND
            ConstraintMode.OR -> ConstraintEntity.MODE_OR
        }
}

object ConstraintEntityMapper {
    private val LENS_MAP =
        mapOf(
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

        val constraintData =
            when (entity.type) {
                ConstraintEntity.APP_FOREGROUND ->
                    ConstraintData.AppInForeground(
                        getPackageName(),
                    )

                ConstraintEntity.APP_NOT_FOREGROUND ->
                    ConstraintData.AppNotInForeground(
                        getPackageName(),
                    )

                ConstraintEntity.APP_PLAYING_MEDIA ->
                    ConstraintData.AppPlayingMedia(
                        getPackageName(),
                    )

                ConstraintEntity.APP_NOT_PLAYING_MEDIA ->
                    ConstraintData.AppNotPlayingMedia(
                        getPackageName(),
                    )

                ConstraintEntity.MEDIA_PLAYING -> ConstraintData.MediaPlaying
                ConstraintEntity.NO_MEDIA_PLAYING -> ConstraintData.NoMediaPlaying

                ConstraintEntity.BT_DEVICE_CONNECTED ->
                    ConstraintData.BtDeviceConnected(
                        getBluetoothAddress(),
                        getBluetoothDeviceName(),
                    )

                ConstraintEntity.BT_DEVICE_DISCONNECTED ->
                    ConstraintData.BtDeviceDisconnected(
                        getBluetoothAddress(),
                        getBluetoothDeviceName(),
                    )

                ConstraintEntity.ORIENTATION_0 ->
                    ConstraintData.OrientationCustom(
                        Orientation.ORIENTATION_0,
                    )

                ConstraintEntity.ORIENTATION_90 ->
                    ConstraintData.OrientationCustom(
                        Orientation.ORIENTATION_90,
                    )

                ConstraintEntity.ORIENTATION_180 ->
                    ConstraintData.OrientationCustom(
                        Orientation.ORIENTATION_180,
                    )

                ConstraintEntity.ORIENTATION_270 ->
                    ConstraintData.OrientationCustom(
                        Orientation.ORIENTATION_270,
                    )

                ConstraintEntity.ORIENTATION_PORTRAIT -> ConstraintData.OrientationPortrait
                ConstraintEntity.ORIENTATION_LANDSCAPE -> ConstraintData.OrientationLandscape

                ConstraintEntity.SCREEN_OFF -> ConstraintData.ScreenOff
                ConstraintEntity.SCREEN_ON -> ConstraintData.ScreenOn

                ConstraintEntity.FLASHLIGHT_ON ->
                    ConstraintData.FlashlightOn(
                        getCameraLens(),
                    )

                ConstraintEntity.FLASHLIGHT_OFF ->
                    ConstraintData.FlashlightOff(
                        getCameraLens(),
                    )

                ConstraintEntity.WIFI_ON -> ConstraintData.WifiOn
                ConstraintEntity.WIFI_OFF -> ConstraintData.WifiOff
                ConstraintEntity.WIFI_CONNECTED -> ConstraintData.WifiConnected(getSsid())
                ConstraintEntity.WIFI_DISCONNECTED ->
                    ConstraintData.WifiDisconnected(
                        getSsid(),
                    )

                ConstraintEntity.IME_CHOSEN ->
                    ConstraintData.ImeChosen(
                        getImeId(),
                        getImeLabel(),
                    )

                ConstraintEntity.IME_NOT_CHOSEN ->
                    ConstraintData.ImeNotChosen(
                        getImeId(),
                        getImeLabel(),
                    )

                ConstraintEntity.DEVICE_IS_UNLOCKED -> ConstraintData.DeviceIsUnlocked
                ConstraintEntity.DEVICE_IS_LOCKED -> ConstraintData.DeviceIsLocked
                ConstraintEntity.LOCK_SCREEN_SHOWING -> ConstraintData.LockScreenShowing
                ConstraintEntity.LOCK_SCREEN_NOT_SHOWING -> ConstraintData.LockScreenNotShowing

                ConstraintEntity.PHONE_RINGING -> ConstraintData.PhoneRinging
                ConstraintEntity.IN_PHONE_CALL -> ConstraintData.InPhoneCall
                ConstraintEntity.NOT_IN_PHONE_CALL -> ConstraintData.NotInPhoneCall

                ConstraintEntity.CHARGING -> ConstraintData.Charging
                ConstraintEntity.DISCHARGING -> ConstraintData.Discharging

                ConstraintEntity.HINGE_CLOSED -> ConstraintData.HingeClosed
                ConstraintEntity.HINGE_OPEN -> ConstraintData.HingeOpen

                ConstraintEntity.TIME -> {
                    val startTime =
                        entity.extras
                            .getData(ConstraintEntity.EXTRA_START_TIME)
                            .valueOrNull()!!
                            .split(":")
                    val startHour = startTime[0].toInt()
                    val startMin = startTime[1].toInt()

                    val endTime =
                        entity.extras
                            .getData(ConstraintEntity.EXTRA_END_TIME)
                            .valueOrNull()!!
                            .split(":")
                    val endHour = endTime[0].toInt()
                    val endMin = endTime[1].toInt()

                    ConstraintData.Time(
                        startHour = startHour,
                        startMinute = startMin,
                        endHour = endHour,
                        endMinute = endMin,
                    )
                }

                else -> throw Exception("don't know how to convert constraint entity with type ${entity.type}")
            }

        return Constraint(
            uid = entity.uid,
            data = constraintData,
        )
    }

    fun toEntity(constraint: Constraint): ConstraintEntity =
        when (constraint.data) {
            is ConstraintData.AppInForeground ->
                ConstraintEntity(
                    uid = constraint.uid,
                    type = ConstraintEntity.APP_FOREGROUND,
                    extras =
                        listOf(
                            EntityExtra(
                                ConstraintEntity.EXTRA_PACKAGE_NAME,
                                constraint.data.packageName,
                            ),
                        ),
                )

            is ConstraintData.AppNotInForeground ->
                ConstraintEntity(
                    uid = constraint.uid,
                    type = ConstraintEntity.APP_NOT_FOREGROUND,
                    extras =
                        listOf(
                            EntityExtra(
                                ConstraintEntity.EXTRA_PACKAGE_NAME,
                                constraint.data.packageName,
                            ),
                        ),
                )

            is ConstraintData.AppPlayingMedia ->
                ConstraintEntity(
                    uid = constraint.uid,
                    type = ConstraintEntity.APP_PLAYING_MEDIA,
                    extras =
                        listOf(
                            EntityExtra(
                                ConstraintEntity.EXTRA_PACKAGE_NAME,
                                constraint.data.packageName,
                            ),
                        ),
                )

            is ConstraintData.AppNotPlayingMedia ->
                ConstraintEntity(
                    uid = constraint.uid,
                    type = ConstraintEntity.APP_NOT_PLAYING_MEDIA,
                    extras =
                        listOf(
                            EntityExtra(
                                ConstraintEntity.EXTRA_PACKAGE_NAME,
                                constraint.data.packageName,
                            ),
                        ),
                )

            is ConstraintData.MediaPlaying ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.MEDIA_PLAYING,
                )

            is ConstraintData.NoMediaPlaying ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.NO_MEDIA_PLAYING,
                )

            is ConstraintData.BtDeviceConnected ->
                ConstraintEntity(
                    uid = constraint.uid,
                    type = ConstraintEntity.BT_DEVICE_CONNECTED,
                    extras =
                        listOf(
                            EntityExtra(ConstraintEntity.EXTRA_BT_ADDRESS, constraint.data.bluetoothAddress),
                            EntityExtra(ConstraintEntity.EXTRA_BT_NAME, constraint.data.deviceName),
                        ),
                )

            is ConstraintData.BtDeviceDisconnected ->
                ConstraintEntity(
                    uid = constraint.uid,
                    type = ConstraintEntity.BT_DEVICE_DISCONNECTED,
                    extras =
                        listOf(
                            EntityExtra(ConstraintEntity.EXTRA_BT_ADDRESS, constraint.data.bluetoothAddress),
                            EntityExtra(ConstraintEntity.EXTRA_BT_NAME, constraint.data.deviceName),
                        ),
                )

            is ConstraintData.OrientationCustom ->
                when (constraint.data.orientation) {
                    Orientation.ORIENTATION_0 ->
                        ConstraintEntity(
                            uid = constraint.uid,
                            ConstraintEntity.ORIENTATION_0,
                        )

                    Orientation.ORIENTATION_90 ->
                        ConstraintEntity(
                            uid = constraint.uid,
                            ConstraintEntity.ORIENTATION_90,
                        )

                    Orientation.ORIENTATION_180 ->
                        ConstraintEntity(
                            uid = constraint.uid,
                            ConstraintEntity.ORIENTATION_180,
                        )

                    Orientation.ORIENTATION_270 ->
                        ConstraintEntity(
                            uid = constraint.uid,
                            ConstraintEntity.ORIENTATION_270,
                        )
                }

            is ConstraintData.OrientationLandscape ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.ORIENTATION_LANDSCAPE,
                )

            is ConstraintData.OrientationPortrait ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.ORIENTATION_PORTRAIT,
                )

            is ConstraintData.ScreenOff ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.SCREEN_OFF,
                )

            is ConstraintData.ScreenOn ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.SCREEN_ON,
                )

            is ConstraintData.FlashlightOff ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.FLASHLIGHT_OFF,
                    EntityExtra(
                        ConstraintEntity.EXTRA_FLASHLIGHT_CAMERA_LENS,
                        LENS_MAP[constraint.data.lens]!!,
                    ),
                )

            is ConstraintData.FlashlightOn ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.FLASHLIGHT_ON,
                    EntityExtra(
                        ConstraintEntity.EXTRA_FLASHLIGHT_CAMERA_LENS,
                        LENS_MAP[constraint.data.lens]!!,
                    ),
                )

            is ConstraintData.WifiConnected -> {
                val extras = mutableListOf<EntityExtra>()

                if (constraint.data.ssid != null) {
                    extras.add(EntityExtra(ConstraintEntity.EXTRA_SSID, constraint.data.ssid))
                }

                ConstraintEntity(
                    uid = constraint.uid,
                    type = ConstraintEntity.WIFI_CONNECTED,
                    extras = extras,
                )
            }

            is ConstraintData.WifiDisconnected -> {
                val extras = mutableListOf<EntityExtra>()

                if (constraint.data.ssid != null) {
                    extras.add(EntityExtra(ConstraintEntity.EXTRA_SSID, constraint.data.ssid))
                }

                ConstraintEntity(
                    uid = constraint.uid,
                    type = ConstraintEntity.WIFI_DISCONNECTED,
                    extras = extras,
                )
            }

            is ConstraintData.WifiOff ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.WIFI_OFF,
                )

            is ConstraintData.WifiOn ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.WIFI_ON,
                )

            is ConstraintData.ImeChosen -> {
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.IME_CHOSEN,
                    EntityExtra(ConstraintEntity.EXTRA_IME_ID, constraint.data.imeId),
                    EntityExtra(ConstraintEntity.EXTRA_IME_LABEL, constraint.data.imeLabel),
                )
            }

            is ConstraintData.ImeNotChosen -> {
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.IME_NOT_CHOSEN,
                    EntityExtra(ConstraintEntity.EXTRA_IME_ID, constraint.data.imeId),
                    EntityExtra(ConstraintEntity.EXTRA_IME_LABEL, constraint.data.imeLabel),
                )
            }

            is ConstraintData.DeviceIsLocked ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.DEVICE_IS_LOCKED,
                )

            is ConstraintData.DeviceIsUnlocked ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.DEVICE_IS_UNLOCKED,
                )

            is ConstraintData.LockScreenShowing ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.LOCK_SCREEN_SHOWING,
                )

            is ConstraintData.LockScreenNotShowing ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.LOCK_SCREEN_NOT_SHOWING,
                )

            is ConstraintData.InPhoneCall ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.IN_PHONE_CALL,
                )

            is ConstraintData.NotInPhoneCall ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.NOT_IN_PHONE_CALL,
                )

            is ConstraintData.PhoneRinging ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.PHONE_RINGING,
                )

            is ConstraintData.Charging ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.CHARGING,
                )

            is ConstraintData.Discharging ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.DISCHARGING,
                )

            is ConstraintData.HingeClosed ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.HINGE_CLOSED,
                )

            is ConstraintData.HingeOpen ->
                ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.HINGE_OPEN,
                )

            is ConstraintData.Time ->
                ConstraintEntity(
                    uid = constraint.uid,
                    type = ConstraintEntity.TIME,
                    EntityExtra(
                        ConstraintEntity.EXTRA_START_TIME,
                        "${constraint.data.startHour}:${constraint.data.startMinute}",
                    ),
                    EntityExtra(
                        ConstraintEntity.EXTRA_END_TIME,
                        "${constraint.data.endHour}:${constraint.data.endMinute}",
                    ),
                )
        }
}
