package io.github.sds100.keymapper

import android.hardware.camera2.CameraCharacteristics
import android.media.AudioManager
import android.os.Build

/**
 * Created by sds100 on 14/04/2019.
 */

object Option {
    //DONT CHANGE THESE IDS!
    const val STREAM_ALARM = "option_stream_alarm"
    const val STREAM_DTMF = "option_stream_dtmf"
    const val STREAM_MUSIC = "option_stream_music"
    const val STREAM_NOTIFICATION = "option_stream_notification"
    const val STREAM_RING = "option_stream_ring"
    const val STREAM_SYSTEM = "option_stream_system"
    const val STREAM_VOICE_CALL = "option_stream_voice_call"

    const val STREAM_ACCESSIBILITY = "option_stream_accessibility"

    const val LENS_FRONT = "option_lens_front"
    const val LENS_BACK = "option_lens_back"

    const val RINGER_MODE_NORMAL = "option_ringer_mode_normal"
    const val RINGER_MODE_VIBRATE = "option_ringer_mode_vibrate"
    const val RINGER_MODE_SILENT = "option_ringer_mode_silent"

    val STREAMS = sequence {
        yieldAll(listOf(STREAM_ALARM,
                STREAM_DTMF,
                STREAM_MUSIC,
                STREAM_NOTIFICATION,
                STREAM_RING,
                STREAM_SYSTEM,
                STREAM_VOICE_CALL
        ))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            yield(STREAM_ACCESSIBILITY)
        }
    }.toList()

    val OPTION_ID_SDK_ID_MAP = sequence {
        yieldAll(listOf(STREAM_ALARM to AudioManager.STREAM_ALARM,
                STREAM_DTMF to AudioManager.STREAM_DTMF,
                STREAM_MUSIC to AudioManager.STREAM_MUSIC,
                STREAM_NOTIFICATION to AudioManager.STREAM_NOTIFICATION,
                STREAM_RING to AudioManager.STREAM_RING,
                STREAM_SYSTEM to AudioManager.STREAM_SYSTEM,
                STREAM_VOICE_CALL to AudioManager.STREAM_VOICE_CALL,

                RINGER_MODE_NORMAL to AudioManager.RINGER_MODE_NORMAL,
                RINGER_MODE_VIBRATE to AudioManager.RINGER_MODE_VIBRATE,
                RINGER_MODE_SILENT to AudioManager.RINGER_MODE_SILENT
        ))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            yield(STREAM_ACCESSIBILITY to AudioManager.STREAM_ACCESSIBILITY)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            yield(LENS_FRONT to CameraCharacteristics.LENS_FACING_FRONT)
            yield(LENS_BACK to CameraCharacteristics.LENS_FACING_BACK)
        }
    }.toMap()

    /**
     * @return the [Extra] id for where the option is stored in the [Action]
     */
    @ExtraId
    fun getExtraIdForOption(id: String): String {
        return when (id) {
            SystemAction.VOLUME_DECREASE_STREAM,
            SystemAction.VOLUME_INCREASE_STREAM -> Action.EXTRA_STREAM_TYPE

            SystemAction.DISABLE_FLASHLIGHT,
            SystemAction.ENABLE_FLASHLIGHT,
            SystemAction.TOGGLE_FLASHLIGHT -> Action.EXTRA_LENS

            SystemAction.CHANGE_RINGER_MODE -> Action.EXTRA_RINGER_MODE

            else -> throw Exception("Can't find an extra id for that system action option $id")
        }
    }
}