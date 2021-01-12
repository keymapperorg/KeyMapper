package io.github.sds100.keymapper.data.model

import android.app.NotificationManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.media.AudioManager
import android.os.Build
import android.view.Surface
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.util.KeyboardUtils
import io.github.sds100.keymapper.util.SystemAction
import io.github.sds100.keymapper.util.result.*
import io.github.sds100.keymapper.util.str

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

    const val DND_ALARMS = "do_not_disturb_alarms"
    const val DND_PRIORITY = "do_not_disturb_priority"
    const val DND_NONE = "do_not_disturb_none"

    const val ROTATION_0 = "rotation_0"
    const val ROTATION_90 = "rotation_90"
    const val ROTATION_180 = "rotation_180"
    const val ROTATION_270 = "rotation_270"

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

    val LENSES = listOf(
        LENS_FRONT,
        LENS_BACK
    )

    val DND_MODES = listOf(
        DND_ALARMS,
        DND_PRIORITY,
        DND_NONE
    )

    val ROTATIONS = listOf(
        ROTATION_0,
        ROTATION_90,
        ROTATION_180,
        ROTATION_270
    )

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
            RINGER_MODE_SILENT to AudioManager.RINGER_MODE_SILENT,

            ROTATION_0 to Surface.ROTATION_0,
            ROTATION_90 to Surface.ROTATION_90,
            ROTATION_180 to Surface.ROTATION_180,
            ROTATION_270 to Surface.ROTATION_270
        ))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            yield(STREAM_ACCESSIBILITY to AudioManager.STREAM_ACCESSIBILITY)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            yield(LENS_FRONT to CameraCharacteristics.LENS_FACING_FRONT)
            yield(LENS_BACK to CameraCharacteristics.LENS_FACING_BACK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            yield(DND_ALARMS to NotificationManager.INTERRUPTION_FILTER_ALARMS)
            yield(DND_PRIORITY to NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            yield(DND_NONE to NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }.toMap()

    /**
     * @return the [Extra] id for where the option is stored in the [Action]
     */
    @ExtraId
    fun getExtraIdForOption(systemActionId: String): String {
        return when (systemActionId) {
            SystemAction.VOLUME_DECREASE_STREAM,
            SystemAction.VOLUME_INCREASE_STREAM -> Action.EXTRA_STREAM_TYPE

            SystemAction.DISABLE_FLASHLIGHT,
            SystemAction.ENABLE_FLASHLIGHT,
            SystemAction.TOGGLE_FLASHLIGHT -> Action.EXTRA_LENS

            SystemAction.CHANGE_RINGER_MODE -> Action.EXTRA_RINGER_MODE

            SystemAction.SWITCH_KEYBOARD -> Action.EXTRA_IME_ID

            SystemAction.TOGGLE_DND_MODE,
            SystemAction.ENABLE_DND_MODE,
            SystemAction.DISABLE_DND_MODE -> Action.EXTRA_DND_MODE

            SystemAction.CYCLE_ROTATIONS -> Action.EXTRA_ORIENTATIONS

            SystemAction.PLAY_MEDIA_PACKAGE,
            SystemAction.PLAY_PAUSE_MEDIA_PACKAGE,
            SystemAction.PAUSE_MEDIA_PACKAGE,
            SystemAction.NEXT_TRACK_PACKAGE,
            SystemAction.PREVIOUS_TRACK_PACKAGE,
            SystemAction.FAST_FORWARD_PACKAGE,
            SystemAction.REWIND_PACKAGE -> Action.EXTRA_PACKAGE_NAME

            else -> throw Exception("Can't find an extra id for that system action $systemActionId")
        }
    }

    fun getOptionLabel(ctx: Context, systemActionId: String, optionId: String): Result<String> {
        when (systemActionId) {
            SystemAction.PLAY_MEDIA_PACKAGE,
            SystemAction.PLAY_PAUSE_MEDIA_PACKAGE,
            SystemAction.PAUSE_MEDIA_PACKAGE,
            SystemAction.NEXT_TRACK_PACKAGE,
            SystemAction.PREVIOUS_TRACK_PACKAGE,
            SystemAction.FAST_FORWARD_PACKAGE,
            SystemAction.REWIND_PACKAGE ->
                return Success(ServiceLocator.packageRepository(ctx).getAppName(optionId))

            SystemAction.SWITCH_KEYBOARD -> {
                return KeyboardUtils.getInputMethodLabel(optionId)
            }
        }

        val label = when (optionId) {
            STREAM_ALARM -> ctx.str(R.string.stream_alarm)
            STREAM_DTMF -> ctx.str(R.string.stream_dtmf)
            STREAM_MUSIC -> ctx.str(R.string.stream_music)
            STREAM_NOTIFICATION -> ctx.str(R.string.stream_notification)
            STREAM_RING -> ctx.str(R.string.stream_ring)
            STREAM_SYSTEM -> ctx.str(R.string.stream_system)
            STREAM_VOICE_CALL -> ctx.str(R.string.stream_voice_call)

            RINGER_MODE_NORMAL -> ctx.str(R.string.ringer_mode_normal)
            RINGER_MODE_VIBRATE -> ctx.str(R.string.ringer_mode_vibrate)
            RINGER_MODE_SILENT -> ctx.str(R.string.ringer_mode_silent)

            LENS_BACK -> ctx.str(R.string.lens_back)
            LENS_FRONT -> ctx.str(R.string.lens_front)

            STREAM_ACCESSIBILITY -> ctx.str(R.string.stream_accessibility)

            DND_ALARMS -> ctx.str(R.string.dnd_mode_alarms)
            DND_PRIORITY -> ctx.str(R.string.dnd_mode_priority)
            DND_NONE -> ctx.str(R.string.dnd_mode_none)

            ROTATION_0 -> ctx.str(R.string.orientation_0)
            ROTATION_90 -> ctx.str(R.string.orientation_90)
            ROTATION_180 -> ctx.str(R.string.orientation_180)
            ROTATION_270 -> ctx.str(R.string.orientation_270)

            else -> return OptionLabelNotFound(optionId)
        }

        return Success(label)
    }

    fun optionSetToString(optionIds: Set<String>) = optionIds.joinToString(",")

    fun optionSetFromString(data: String): Result<Set<String>> =
        try {
            Success(data.split(',').toSet())
        } catch (e: Exception) {
            FailedToSplitString(data)
        }

    fun labelsFromOptionSet(ctx: Context, systemActionId: String, optionSet: Set<String>): Result<Set<String>> {
        val labels = mutableSetOf<String>()

        loop@ for (optionId in optionSet) {
            getOptionLabel(ctx, systemActionId, optionId).onSuccess {
                labels.add(it)
            }.onFailure { return@labelsFromOptionSet it }
        }

        return Success(labels)
    }
}

enum class OptionType {
    SINGLE, MULTIPLE
}