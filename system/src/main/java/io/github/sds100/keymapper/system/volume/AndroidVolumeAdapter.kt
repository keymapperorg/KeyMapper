package io.github.sds100.keymapper.system.volume

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.otherwise
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.isConnected
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidVolumeAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager
) : VolumeAdapter {
    private val ctx = context.applicationContext

    private val audioManager: AudioManager by lazy { ctx.getSystemService()!! }
    private val notificationManager: NotificationManager by lazy { ctx.getSystemService()!! }

    override val ringerMode: RingerMode
        get() {
            // Get the current ringer mode with the setting because the AudioManager
            // always returns the same value when the value has been changed by the
            // the system bridge.
            val ringerModeSdk = if (systemBridgeConnectionManager.isConnected()) {
                SettingsUtils.getGlobalSetting<Int>(
                    ctx,
                    Settings.Global.MODE_RINGER
                ) ?: 0
            } else {
                audioManager.ringerMode
            }

            return when (ringerModeSdk) {
                AudioManager.RINGER_MODE_NORMAL -> RingerMode.NORMAL
                AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
                AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
                else -> throw Exception("Don't know how to convert this ringer moder ${audioManager.ringerMode}")
            }
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

            return if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
                systemBridgeConnectionManager.run { systemBridge ->
                    systemBridge.setRingerMode(sdkMode)
                }.otherwise {
                    Timber.e("Failed to set ringer mode with System Bridge, falling back to AudioManager")

                    audioManager.ringerMode = sdkMode
                    Success(Unit)
                }
            } else {
                audioManager.ringerMode = sdkMode
                Success(Unit)
            }
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

    override fun muteMicrophone(): KMResult<*> {
        audioManager.isMicrophoneMute = true
        return Success(Unit)
    }

    override fun unmuteMicrophone(): KMResult<*> {
        audioManager.isMicrophoneMute = false
        return Success(Unit)
    }

    override fun toggleMuteMicrophone(): KMResult<*> {
        audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute
        return Success(Unit)
    }

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
