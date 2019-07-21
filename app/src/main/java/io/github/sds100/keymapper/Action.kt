package io.github.sds100.keymapper

import android.hardware.camera2.CameraCharacteristics
import android.media.AudioManager
import androidx.annotation.StringDef
import androidx.room.ColumnInfo
import io.github.sds100.keymapper.data.KeyMapDao
import io.github.sds100.keymapper.util.ErrorCodeUtils
import java.io.Serializable

/**
 * Created by sds100 on 16/07/2018.
 */

@StringDef(value = [
    Action.EXTRA_PACKAGE_NAME,
    Action.EXTRA_SHORTCUT_TITLE,
    Action.EXTRA_STREAM_TYPE
])
annotation class ExtraId

/**
 * @property [data] The information required to perform the action. E.g if the type is [ActionType.APP],
 * the data will be the package name of the application
 *
 * Different Types of actions:
 * - Apps
 * - App shortcuts
 * - Keycode
 * - Key
 * - Insert a block of text
 * - System actions/settings
 */
data class Action(
        @ColumnInfo(name = KeyMapDao.KEY_ACTION_TYPE)
        val type: ActionType,

        /**
         * How each action type saves data:
         *
         * - Apps: package name
         * - App shortcuts: the intent for the shortcut as a parsed URI
         * - Keycode: the keycode
         * - Key: the keycode of the key
         * - Block of text: text to insert
         * - System action: the system action id
         */
        @ColumnInfo(name = KeyMapDao.KEY_ACTION_DATA)
        val data: String,

        @ColumnInfo(name = KeyMapDao.KEY_ACTION_EXTRAS)
        val extras: MutableList<Extra> = mutableListOf()
) : Serializable {
    companion object {
        const val EXTRA_ACTION = "extra_action"

        //DON'T CHANGE THESE IDs!!!!
        const val EXTRA_SHORTCUT_TITLE = "extra_title"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_STREAM_TYPE = "extra_stream_type"
        const val EXTRA_LENS = "extra_flash"
        const val EXTRA_RINGER_MODE = "extra_ringer_mode"
    }

    constructor(type: ActionType, data: String, extra: Extra) : this(type, data, mutableListOf(extra))

    fun getExtraData(extraId: String): Result<String> {
        migrateExtra(extraId)

        return extras.find { it.id == extraId }?.data.result(
                ErrorCodeUtils.ERROR_CODE_ACTION_EXTRA_NOT_FOUND, extraId
        )
    }

    private fun migrateExtra(extraId: String) {
        //migrate old system action options to new ones (SDK int to key mapper string id)
        extras.find { it.id == extraId }?.data?.toIntOrNull().let { extraValueInt ->
            when (data) {
                SystemAction.VOLUME_DECREASE_STREAM, SystemAction.VOLUME_INCREASE_STREAM ->
                    when (extraValueInt) {
                        AudioManager.STREAM_ALARM -> Option.STREAM_ALARM
                        AudioManager.STREAM_DTMF -> Option.STREAM_DTMF
                        AudioManager.STREAM_MUSIC -> Option.STREAM_MUSIC
                        AudioManager.STREAM_NOTIFICATION -> Option.STREAM_NOTIFICATION
                        AudioManager.STREAM_RING -> Option.STREAM_RING
                        AudioManager.STREAM_SYSTEM -> Option.STREAM_SYSTEM
                        AudioManager.STREAM_VOICE_CALL -> Option.STREAM_VOICE_CALL
                        AudioManager.STREAM_ACCESSIBILITY -> Option.STREAM_ACCESSIBILITY
                        else -> null
                    }

                SystemAction.TOGGLE_FLASHLIGHT, SystemAction.ENABLE_FLASHLIGHT, SystemAction.DISABLE_FLASHLIGHT ->
                    when (extraValueInt) {
                        CameraCharacteristics.LENS_FACING_FRONT -> Option.LENS_FRONT
                        CameraCharacteristics.LENS_FACING_BACK -> Option.LENS_BACK
                        else -> null
                    }

                else -> null

            }?.let { newExtraValue ->
                extras.removeAll { it.id == extraId }
                extras.add(Extra(extraId, newExtraValue))
            }
        }
    }
}

val Action?.requiresIME: Boolean
    get() {
        if (this == null) return false

        return type == ActionType.KEY ||
                type == ActionType.KEYCODE ||
                type == ActionType.TEXT_BLOCK ||
                data == SystemAction.MOVE_CURSOR_TO_END
    }

val Action?.isVolumeAction: Boolean
    get() {
        if (this == null) return false

        return listOf(
                SystemAction.VOLUME_DECREASE_STREAM,
                SystemAction.VOLUME_INCREASE_STREAM,
                SystemAction.VOLUME_DOWN,
                SystemAction.VOLUME_UP,
                SystemAction.VOLUME_MUTE,
                SystemAction.VOLUME_TOGGLE_MUTE,
                SystemAction.VOLUME_UNMUTE
        ).contains(data)
    }