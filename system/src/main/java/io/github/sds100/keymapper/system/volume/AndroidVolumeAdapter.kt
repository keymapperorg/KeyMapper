package io.github.sds100.keymapper.system.volume

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.common.util.result.Error
import io.github.sds100.keymapper.common.util.result.Result
import io.github.sds100.keymapper.common.util.result.Success
import io.github.sds100.keymapper.common.util.result.then
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidVolumeAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : VolumeAdapter {
    private val ctx = context.applicationContext

    private val audioManager: AudioManager by lazy { ctx.getSystemService()!! }
    private val notificationManager: NotificationManager by lazy { ctx.getSystemService()!! }

    override val ringerMode: RingerMode
        get() = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> RingerMode.NORMAL
            AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
            AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
            else -> throw Exception("Don't know how to convert this ringer moder ${audioManager.ringerMode}")
        }

    override fun raiseVolume(stream: VolumeStream?, showVolumeUi: Boolean): Result<*> =
        stream.convert().then { streamType ->
            adjustVolume(AudioManager.ADJUST_RAISE, showVolumeUi, streamType)
        }

    override fun lowerVolume(stream: VolumeStream?, showVolumeUi: Boolean): Result<*> =
        stream.convert().then { streamType ->
            adjustVolume(AudioManager.ADJUST_LOWER, showVolumeUi, streamType)
        }

    override fun muteVolume(stream: VolumeStream?, showVolumeUi: Boolean): Result<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return stream.convert().then { streamType ->
                adjustVolume(AudioManager.ADJUST_MUTE, showVolumeUi, streamType)
            }
        } else {
            return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
        }
    }

    override fun unmuteVolume(stream: VolumeStream?, showVolumeUi: Boolean): Result<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return stream.convert().then { streamType ->
                adjustVolume(AudioManager.ADJUST_UNMUTE, showVolumeUi, streamType)
            }
        } else {
            return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
        }
    }

    override fun toggleMuteVolume(stream: VolumeStream?, showVolumeUi: Boolean): Result<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return stream.convert().then { streamType ->
                adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE, showVolumeUi, streamType)
            }
        } else {
            return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
        }
    }

    override fun showVolumeUi(): Result<*> =
        adjustVolume(AudioManager.ADJUST_SAME, showVolumeUi = true)

    override fun setRingerMode(mode: RingerMode): Result<*> {
        try {
            val sdkMode = when (mode) {
                RingerMode.NORMAL -> AudioManager.RINGER_MODE_NORMAL
                RingerMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                RingerMode.SILENT -> AudioManager.RINGER_MODE_SILENT
            }

            audioManager.ringerMode = sdkMode

            return Success(Unit)
        } catch (e: SecurityException) {
            return SystemError.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY)
        }
    }

    override fun enableDndMode(dndMode: DndMode): Result<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.setInterruptionFilter(dndMode.convert())
            return Success(Unit)
        }

        return Error.SdkVersionTooLow(Build.VERSION_CODES.M)
    }

    override fun disableDndMode(): Result<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            return Success(Unit)
        }

        return Error.SdkVersionTooLow(Build.VERSION_CODES.M)
    }

    override fun isDndEnabled(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    } else {
        false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun DndMode.convert(): Int = when (this) {
        DndMode.ALARMS -> NotificationManager.INTERRUPTION_FILTER_ALARMS
        DndMode.PRIORITY -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
        DndMode.NONE -> NotificationManager.INTERRUPTION_FILTER_NONE
    }

    private fun VolumeStream?.convert(): Result<Int?> = when (this) {
        VolumeStream.ALARM -> Success(AudioManager.STREAM_ALARM)
        VolumeStream.DTMF -> Success(AudioManager.STREAM_DTMF)
        VolumeStream.MUSIC -> Success(AudioManager.STREAM_MUSIC)
        VolumeStream.NOTIFICATION -> Success(AudioManager.STREAM_NOTIFICATION)
        VolumeStream.RING -> Success(AudioManager.STREAM_RING)
        VolumeStream.SYSTEM -> Success(AudioManager.STREAM_SYSTEM)
        VolumeStream.VOICE_CALL -> Success(AudioManager.STREAM_VOICE_CALL)
        VolumeStream.ACCESSIBILITY ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Success(AudioManager.STREAM_ACCESSIBILITY)
            } else {
                Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.O)
            }

        null -> Success(null)
    }

    private fun adjustVolume(
        adjustMode: Int,
        showVolumeUi: Boolean = false,
        streamType: Int? = null,
    ): Result<*> {
        try {
            val flag = if (showVolumeUi) {
                AudioManager.FLAG_SHOW_UI
            } else {
                0
            }

            if (streamType == null) {
                audioManager.adjustVolume(adjustMode, flag)
            } else {
                audioManager.adjustStreamVolume(streamType, adjustMode, flag)
            }

            return Success(Unit)
        } catch (e: SecurityException) {
            return SystemError.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY)
        }
    }
}
