package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.serialization.Serializable
import java.util.*

/**
 * Created by sds100 on 03/03/2021.
 */

@Serializable
sealed class Constraint {
    val uid: String = UUID.randomUUID().toString()

    @Serializable
    data class AppInForeground(val packageName: String) : Constraint()

    @Serializable
    data class AppNotInForeground(val packageName: String) : Constraint()

    @Serializable
    data class AppPlayingMedia(val packageName: String) : Constraint()

    @Serializable
    data class BtDeviceConnected(val bluetoothAddress: String, val deviceName: String) :
        Constraint()

    @Serializable
    data class BtDeviceDisconnected(val bluetoothAddress: String, val deviceName: String) :
        Constraint()

    @Serializable
    object ScreenOn : Constraint()

    @Serializable
    object ScreenOff : Constraint()

    @Serializable
    object OrientationPortrait : Constraint()

    @Serializable
    object OrientationLandscape : Constraint()

    @Serializable
    data class OrientationCustom(val orientation: Orientation) : Constraint()
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
    fun fromEntity(entity: ConstraintEntity): Constraint {
        fun getPackageName(): String {
            return entity.extras.getData(ConstraintEntity.EXTRA_PACKAGE_NAME).valueOrNull()!!
        }

        fun getBluetoothAddress(): String {
            return entity.extras.getData(ConstraintEntity.EXTRA_BT_ADDRESS).valueOrNull()!!
        }

        fun getBluetoothDeviceName(): String {
            return entity.extras.getData(ConstraintEntity.EXTRA_BT_NAME).valueOrNull()!!
        }

        return when (entity.type) {
            ConstraintEntity.APP_FOREGROUND -> Constraint.AppInForeground(getPackageName())
            ConstraintEntity.APP_NOT_FOREGROUND -> Constraint.AppNotInForeground(getPackageName())
            ConstraintEntity.APP_PLAYING_MEDIA -> Constraint.AppPlayingMedia(getPackageName())

            ConstraintEntity.BT_DEVICE_CONNECTED ->
                Constraint.BtDeviceConnected(getBluetoothAddress(), getBluetoothDeviceName())

            ConstraintEntity.BT_DEVICE_DISCONNECTED ->
                Constraint.BtDeviceConnected(getBluetoothAddress(), getBluetoothDeviceName())

            ConstraintEntity.ORIENTATION_0 -> Constraint.OrientationCustom(Orientation.ORIENTATION_0)
            ConstraintEntity.ORIENTATION_90 -> Constraint.OrientationCustom(Orientation.ORIENTATION_90)
            ConstraintEntity.ORIENTATION_180 -> Constraint.OrientationCustom(Orientation.ORIENTATION_180)
            ConstraintEntity.ORIENTATION_270 -> Constraint.OrientationCustom(Orientation.ORIENTATION_270)

            ConstraintEntity.ORIENTATION_PORTRAIT -> Constraint.OrientationPortrait
            ConstraintEntity.ORIENTATION_LANDSCAPE -> Constraint.OrientationLandscape

            ConstraintEntity.SCREEN_OFF -> Constraint.ScreenOff
            ConstraintEntity.SCREEN_ON -> Constraint.ScreenOn

            else -> throw Exception("don't know how to convert constraint entity with type ${entity.type}")
        }
    }

    fun toEntity(constraint: Constraint): ConstraintEntity = when (constraint) {
        is Constraint.AppInForeground -> ConstraintEntity(
            type = ConstraintEntity.APP_FOREGROUND,
            extras = listOf(Extra(ConstraintEntity.EXTRA_PACKAGE_NAME, constraint.packageName))
        )

        is Constraint.AppNotInForeground -> ConstraintEntity(
            type = ConstraintEntity.APP_NOT_FOREGROUND,
            extras = listOf(Extra(ConstraintEntity.EXTRA_PACKAGE_NAME, constraint.packageName))
        )

        is Constraint.AppPlayingMedia -> ConstraintEntity(
            type = ConstraintEntity.APP_PLAYING_MEDIA,
            extras = listOf(Extra(ConstraintEntity.EXTRA_PACKAGE_NAME, constraint.packageName))
        )

        is Constraint.BtDeviceConnected -> ConstraintEntity(
            type = ConstraintEntity.BT_DEVICE_CONNECTED,
            extras = listOf(
                Extra(ConstraintEntity.EXTRA_BT_ADDRESS, constraint.bluetoothAddress),
                Extra(ConstraintEntity.EXTRA_BT_NAME, constraint.deviceName),
            )
        )

        is Constraint.BtDeviceDisconnected -> ConstraintEntity(
            type = ConstraintEntity.BT_DEVICE_DISCONNECTED,
            extras = listOf(
                Extra(ConstraintEntity.EXTRA_BT_ADDRESS, constraint.bluetoothAddress),
                Extra(ConstraintEntity.EXTRA_BT_NAME, constraint.deviceName),
            )
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
    }
}