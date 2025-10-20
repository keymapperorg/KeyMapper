package io.github.sds100.keymapper.system.volume

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
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

    override fun raiseVolume(stream: VolumeStream?, showVolumeUi: Boolean): KMResult<*> =
        stream.convert().then { streamType ->
            adjustVolume(AudioManager.ADJUST_RAISE, showVolumeUi, streamType)
        }

    override fun lowerVolume(stream: VolumeStream?, showVolumeUi: Boolean): KMResult<*> =
        stream.convert().then { streamType ->
            adjustVolume(AudioManager.ADJUST_LOWER, showVolumeUi, streamType)
        }

    override fun muteVolume(stream: VolumeStream?, showVolumeUi: Boolean): KMResult<*> {
        return stream.convert().then { streamType ->
            adjustVolume(AudioManager.ADJUST_MUTE, showVolumeUi, streamType)
        }
    }

    override fun unmuteVolume(stream: VolumeStream?, showVolumeUi: Boolean): KMResult<*> {
        return stream.convert().then { streamType ->
            adjustVolume(AudioManager.ADJUST_UNMUTE, showVolumeUi, streamType)
        }
    }

    override fun toggleMuteVolume(stream: VolumeStream?, showVolumeUi: Boolean): KMResult<*> {
        return stream.convert().then { streamType ->
            adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE, showVolumeUi, streamType)
        }
    }

    override fun showVolumeUi(): KMResult<*> =
        adjustVolume(AudioManager.ADJUST_SAME, showVolumeUi = true)

    override fun setRingerMode(mode: RingerMode): KMResult<*> {
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

    override fun enableDndMode(dndMode: DndMode): KMResult<*> {
        notificationManager.setInterruptionFilter(dndMode.convert())
        return Success(Unit)

    }

    override fun disableDndMode(): KMResult<*> {
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        return Success(Unit)

    }

    override fun isDndEnabled(): Boolean =
        notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

    private fun DndMode.convert(): Int = when (this) {
        DndMode.ALARMS -> NotificationManager.INTERRUPTION_FILTER_ALARMS
        DndMode.PRIORITY -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
        DndMode.NONE -> NotificationManager.INTERRUPTION_FILTER_NONE
    }

    private fun VolumeStream?.convert(): KMResult<Int?> = when (this) {
        VolumeStream.ALARM -> Success(AudioManager.STREAM_ALARM)
        VolumeStream.DTMF -> Success(AudioManager.STREAM_DTMF)
        VolumeStream.MUSIC -> Success(AudioManager.STREAM_MUSIC)
        VolumeStream.NOTIFICATION -> Success(AudioManager.STREAM_NOTIFICATION)
        VolumeStream.RING -> Success(AudioManager.STREAM_RING)
        VolumeStream.SYSTEM -> Success(AudioManager.STREAM_SYSTEM)
        VolumeStream.VOICE_CALL -> Success(AudioManager.STREAM_VOICE_CALL)
        VolumeStream.ACCESSIBILITY -> Success(AudioManager.STREAM_ACCESSIBILITY)

        null -> Success(null)
    }

    private fun adjustVolume(
        adjustMode: Int,
        showVolumeUi: Boolean = false,
        streamType: Int? = null,
    ): KMResult<*> {
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
