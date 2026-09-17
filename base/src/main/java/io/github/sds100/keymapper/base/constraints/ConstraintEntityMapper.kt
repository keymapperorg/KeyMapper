package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.PhysicalOrientation
import io.github.sds100.keymapper.common.utils.getKey
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.volume.RingerMode

object ConstraintEntityMapper {

    private val LENS_MAP = mapOf(
        CameraLens.BACK to "option_lens_back",
        CameraLens.FRONT to "option_lens_front",
    )

    /**
     * These entity types were replaced by the positive constraint with the NOT extra.
     */
    @Suppress("DEPRECATION")
    private val NOT_ENTITY_TYPES = setOf(
        ConstraintEntity.APP_NOT_FOREGROUND,
        ConstraintEntity.APP_NOT_PLAYING_MEDIA,
        ConstraintEntity.NO_MEDIA_PLAYING,
        ConstraintEntity.BT_DEVICE_DISCONNECTED,
        ConstraintEntity.SCREEN_OFF,
        ConstraintEntity.FLASHLIGHT_OFF,
        ConstraintEntity.WIFI_OFF,
        ConstraintEntity.WIFI_DISCONNECTED,
        ConstraintEntity.IME_NOT_CHOSEN,
        ConstraintEntity.KEYBOARD_NOT_SHOWING,
        ConstraintEntity.DEVICE_IS_UNLOCKED,
        ConstraintEntity.LOCK_SCREEN_NOT_SHOWING,
        ConstraintEntity.DISCHARGING,
        ConstraintEntity.NOTIFICATION_PANEL_NOT_SHOWING,
    )

    @Suppress("DEPRECATION")
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

        fun getResolutionWidth(): Int =
            entity.extras.getData(ConstraintEntity.EXTRA_RESOLUTION_WIDTH).valueOrNull()!!.toInt()

        fun getResolutionHeight(): Int =
            entity.extras.getData(ConstraintEntity.EXTRA_RESOLUTION_HEIGHT).valueOrNull()!!.toInt()

        val constraintData = when (entity.type) {
            ConstraintEntity.APP_FOREGROUND,
            ConstraintEntity.APP_NOT_FOREGROUND,
                -> ConstraintData.AppInForeground(getPackageName())

            ConstraintEntity.APP_PLAYING_MEDIA,
            ConstraintEntity.APP_NOT_PLAYING_MEDIA,
                -> ConstraintData.AppPlayingMedia(getPackageName())

            ConstraintEntity.MEDIA_PLAYING,
            ConstraintEntity.NO_MEDIA_PLAYING,
                -> ConstraintData.MediaPlaying

            ConstraintEntity.BT_DEVICE_CONNECTED,
            ConstraintEntity.BT_DEVICE_DISCONNECTED,
                -> ConstraintData.BtDeviceConnected(
                    getBluetoothAddress(),
                    getBluetoothDeviceName(),
                )

            ConstraintEntity.ORIENTATION_0 -> ConstraintData.OrientationCustom(
                Orientation.ORIENTATION_0,
            )

            ConstraintEntity.ORIENTATION_90 -> ConstraintData.OrientationCustom(
                Orientation.ORIENTATION_90,
            )

            ConstraintEntity.ORIENTATION_180 -> ConstraintData.OrientationCustom(
                Orientation.ORIENTATION_180,
            )

            ConstraintEntity.ORIENTATION_270 -> ConstraintData.OrientationCustom(
                Orientation.ORIENTATION_270,
            )

            ConstraintEntity.ORIENTATION_PORTRAIT -> ConstraintData.OrientationPortrait

            ConstraintEntity.ORIENTATION_LANDSCAPE -> ConstraintData.OrientationLandscape

            ConstraintEntity.PHYSICAL_ORIENTATION_PORTRAIT ->
                ConstraintData.PhysicalOrientation(PhysicalOrientation.PORTRAIT)

            ConstraintEntity.PHYSICAL_ORIENTATION_LANDSCAPE ->
                ConstraintData.PhysicalOrientation(PhysicalOrientation.LANDSCAPE)

            ConstraintEntity.PHYSICAL_ORIENTATION_PORTRAIT_INVERTED ->
                ConstraintData.PhysicalOrientation(PhysicalOrientation.PORTRAIT_INVERTED)

            ConstraintEntity.PHYSICAL_ORIENTATION_LANDSCAPE_INVERTED ->
                ConstraintData.PhysicalOrientation(PhysicalOrientation.LANDSCAPE_INVERTED)

            ConstraintEntity.DISPLAY_RESOLUTION -> ConstraintData.DisplayResolution(
                width = getResolutionWidth(),
                height = getResolutionHeight(),
            )

            ConstraintEntity.SCREEN_ON,
            ConstraintEntity.SCREEN_OFF,
                -> ConstraintData.ScreenOn

            ConstraintEntity.FLASHLIGHT_ON,
            ConstraintEntity.FLASHLIGHT_OFF,
                -> ConstraintData.FlashlightOn(getCameraLens())

            ConstraintEntity.WIFI_ON,
            ConstraintEntity.WIFI_OFF,
                -> ConstraintData.WifiOn

            ConstraintEntity.WIFI_CONNECTED,
            ConstraintEntity.WIFI_DISCONNECTED,
                -> ConstraintData.WifiConnected(getSsid())

            ConstraintEntity.IME_CHOSEN,
            ConstraintEntity.IME_NOT_CHOSEN,
                -> ConstraintData.ImeChosen(
                    getImeId(),
                    getImeLabel(),
                )

            ConstraintEntity.KEYBOARD_SHOWING,
            ConstraintEntity.KEYBOARD_NOT_SHOWING,
                -> ConstraintData.KeyboardShowing

            ConstraintEntity.DEVICE_IS_LOCKED,
            ConstraintEntity.DEVICE_IS_UNLOCKED,
                -> ConstraintData.DeviceIsLocked

            ConstraintEntity.LOCK_SCREEN_SHOWING,
            ConstraintEntity.LOCK_SCREEN_NOT_SHOWING,
                -> ConstraintData.LockScreenShowing

            ConstraintEntity.PHONE_RINGING -> ConstraintData.PhoneRinging

            ConstraintEntity.IN_PHONE_CALL -> ConstraintData.InPhoneCall

            ConstraintEntity.NOT_IN_PHONE_CALL -> ConstraintData.NotInPhoneCall

            ConstraintEntity.RINGER_MODE_NORMAL ->
                ConstraintData.RingerMode(RingerMode.NORMAL)

            ConstraintEntity.RINGER_MODE_VIBRATE ->
                ConstraintData.RingerMode(RingerMode.VIBRATE)

            ConstraintEntity.RINGER_MODE_SILENT ->
                ConstraintData.RingerMode(RingerMode.SILENT)

            ConstraintEntity.CHARGING,
            ConstraintEntity.DISCHARGING,
                -> ConstraintData.Charging

            ConstraintEntity.HINGE_CLOSED -> ConstraintData.HingeClosed

            ConstraintEntity.HINGE_OPEN -> ConstraintData.HingeOpen

            ConstraintEntity.NOTIFICATION_PANEL_SHOWING,
            ConstraintEntity.NOTIFICATION_PANEL_NOT_SHOWING,
                -> ConstraintData.NotificationPanelShowing

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

                ConstraintData.Time(
                    startHour = startHour,
                    startMinute = startMin,
                    endHour = endHour,
                    endMinute = endMin,
                )
            }

            else -> throw Exception(
                "don't know how to convert constraint entity with type ${entity.type}",
            )
        }

        val isNotType = entity.type in NOT_ENTITY_TYPES

        return Constraint(
            uid = entity.uid,
            data = constraintData,
            isNot = isNotType != entity.isNot,
        )
    }

    fun toEntity(constraint: Constraint): ConstraintEntity =
        toEntityWithoutNot(constraint).copy(isNot = constraint.isNot)

    private fun toEntityWithoutNot(constraint: Constraint): ConstraintEntity =
        when (constraint.data) {
            is ConstraintData.AppInForeground -> ConstraintEntity(
                uid = constraint.uid,
                type = ConstraintEntity.APP_FOREGROUND,
                extras = listOf(
                    EntityExtra(
                        ConstraintEntity.EXTRA_PACKAGE_NAME,
                        constraint.data.packageName,
                    ),
                ),
            )

            is ConstraintData.AppPlayingMedia -> ConstraintEntity(
                uid = constraint.uid,
                type = ConstraintEntity.APP_PLAYING_MEDIA,
                extras = listOf(
                    EntityExtra(
                        ConstraintEntity.EXTRA_PACKAGE_NAME,
                        constraint.data.packageName,
                    ),
                ),
            )

            is ConstraintData.MediaPlaying -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.MEDIA_PLAYING,
            )

            is ConstraintData.BtDeviceConnected -> ConstraintEntity(
                uid = constraint.uid,
                type = ConstraintEntity.BT_DEVICE_CONNECTED,
                extras = listOf(
                    EntityExtra(
                        ConstraintEntity.EXTRA_BT_ADDRESS,
                        constraint.data.bluetoothAddress,
                    ),
                    EntityExtra(ConstraintEntity.EXTRA_BT_NAME, constraint.data.deviceName),
                ),
            )

            is ConstraintData.OrientationCustom -> when (constraint.data.orientation) {
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

            is ConstraintData.OrientationLandscape -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.ORIENTATION_LANDSCAPE,
            )

            is ConstraintData.OrientationPortrait -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.ORIENTATION_PORTRAIT,
            )

            is ConstraintData.PhysicalOrientation -> when (constraint.data.physicalOrientation) {
                PhysicalOrientation.PORTRAIT -> ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.PHYSICAL_ORIENTATION_PORTRAIT,
                )

                PhysicalOrientation.LANDSCAPE -> ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.PHYSICAL_ORIENTATION_LANDSCAPE,
                )

                PhysicalOrientation.PORTRAIT_INVERTED -> ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.PHYSICAL_ORIENTATION_PORTRAIT_INVERTED,
                )

                PhysicalOrientation.LANDSCAPE_INVERTED -> ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.PHYSICAL_ORIENTATION_LANDSCAPE_INVERTED,
                )
            }

            is ConstraintData.DisplayResolution -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.DISPLAY_RESOLUTION,
                EntityExtra(
                    ConstraintEntity.EXTRA_RESOLUTION_WIDTH,
                    constraint.data.width.toString(),
                ),
                EntityExtra(
                    ConstraintEntity.EXTRA_RESOLUTION_HEIGHT,
                    constraint.data.height.toString(),
                ),
            )

            is ConstraintData.ScreenOn -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.SCREEN_ON,
            )

            is ConstraintData.FlashlightOn -> ConstraintEntity(
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

            is ConstraintData.WifiOn -> ConstraintEntity(
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

            is ConstraintData.KeyboardShowing -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.KEYBOARD_SHOWING,
            )

            is ConstraintData.DeviceIsLocked -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.DEVICE_IS_LOCKED,
            )

            is ConstraintData.LockScreenShowing -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.LOCK_SCREEN_SHOWING,
            )

            is ConstraintData.InPhoneCall -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.IN_PHONE_CALL,
            )

            is ConstraintData.NotInPhoneCall -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.NOT_IN_PHONE_CALL,
            )

            is ConstraintData.PhoneRinging -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.PHONE_RINGING,
            )

            is ConstraintData.RingerMode -> when (constraint.data.ringerMode) {
                RingerMode.NORMAL -> ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.RINGER_MODE_NORMAL,
                )

                RingerMode.VIBRATE -> ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.RINGER_MODE_VIBRATE,
                )

                RingerMode.SILENT -> ConstraintEntity(
                    uid = constraint.uid,
                    ConstraintEntity.RINGER_MODE_SILENT,
                )
            }

            is ConstraintData.Charging -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.CHARGING,
            )

            is ConstraintData.HingeClosed -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.HINGE_CLOSED,
            )

            is ConstraintData.HingeOpen -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.HINGE_OPEN,
            )

            is ConstraintData.NotificationPanelShowing -> ConstraintEntity(
                uid = constraint.uid,
                ConstraintEntity.NOTIFICATION_PANEL_SHOWING,
            )

            is ConstraintData.Time -> ConstraintEntity(
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
