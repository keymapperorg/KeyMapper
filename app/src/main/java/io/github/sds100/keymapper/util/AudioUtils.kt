package io.github.sds100.keymapper.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import splitties.systemservices.audioManager
import splitties.systemservices.notificationManager

/**
 * Created by sds100 on 21/10/2018.
 */

object AudioUtils {
    @SuppressLint("InlinedApi")
    @IntDef(value = [
        AudioManager.ADJUST_LOWER,
        AudioManager.ADJUST_RAISE,
        AudioManager.ADJUST_SAME,

        AudioManager.ADJUST_MUTE,
        AudioManager.ADJUST_UNMUTE,
        AudioManager.ADJUST_TOGGLE_MUTE
    ])
    @Retention(AnnotationRetention.SOURCE)
    annotation class AdjustMode

    @SuppressLint("InlinedApi")
    @IntDef(value = [
        AudioManager.STREAM_ACCESSIBILITY,
        AudioManager.STREAM_ALARM,
        AudioManager.STREAM_DTMF,
        AudioManager.STREAM_MUSIC,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_RING,
        AudioManager.STREAM_SYSTEM,
        AudioManager.STREAM_VOICE_CALL])
    @Retention(AnnotationRetention.SOURCE)
    annotation class StreamType

    @IntDef(value = [
        AudioManager.RINGER_MODE_NORMAL,
        AudioManager.RINGER_MODE_VIBRATE,
        AudioManager.RINGER_MODE_SILENT
    ])
    annotation class RingerMode

    /**
     * @param adjustMode must be one of the AudioManager.ADJUST... values
     */
    fun adjustVolume(ctx: Context,
                     @AdjustMode adjustMode: Int,
                     showVolumeUi: Boolean = false) {

        if (PermissionUtils.isPermissionGranted(Manifest.permission.ACCESS_NOTIFICATION_POLICY)) {

            val audioManager = ctx.applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager

            val flag = if (showVolumeUi) {
                AudioManager.FLAG_SHOW_UI
            } else {
                0
            }

            audioManager.adjustVolume(adjustMode, flag)
        }
    }

    fun adjustSpecificStream(ctx: Context,
                             @AdjustMode adjustMode: Int,
                             showVolumeUi: Boolean = false,
                             @StreamType streamType: Int) {

        if (PermissionUtils.isPermissionGranted(Manifest.permission.ACCESS_NOTIFICATION_POLICY)) {
            val audioManager = ctx.applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager

            val flag = if (showVolumeUi) {
                AudioManager.FLAG_SHOW_UI
            } else {
                0
            }

            audioManager.adjustStreamVolume(streamType, adjustMode, flag)
        }
    }

    fun cycleBetweenVibrateAndRing(ctx: Context) {
        audioManager.apply {
            when (ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> changeRingerMode(ctx, AudioManager.RINGER_MODE_VIBRATE)
                AudioManager.RINGER_MODE_VIBRATE -> changeRingerMode(ctx, AudioManager.RINGER_MODE_NORMAL)
                AudioManager.RINGER_MODE_SILENT -> changeRingerMode(ctx, AudioManager.RINGER_MODE_NORMAL)
            }
        }
    }

    fun cycleThroughAllRingerModes(ctx: Context) {
        audioManager.apply {

            when (ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> changeRingerMode(ctx, AudioManager.RINGER_MODE_VIBRATE)
                AudioManager.RINGER_MODE_VIBRATE -> changeRingerMode(ctx, AudioManager.RINGER_MODE_SILENT)
                AudioManager.RINGER_MODE_SILENT -> changeRingerMode(ctx, AudioManager.RINGER_MODE_NORMAL)
            }
        }
    }

    fun changeRingerMode(ctx: Context, @RingerMode ringerMode: Int) {
        if (PermissionUtils.isPermissionGranted(Manifest.permission.ACCESS_NOTIFICATION_POLICY)) {
            val audioManager = ctx.applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager

            audioManager.ringerMode = ringerMode
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun toggleDndMode(mode: Int) {
        if (notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        } else {
            notificationManager.setInterruptionFilter(mode)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun enableDndMode(mode: Int) {
        notificationManager.setInterruptionFilter(mode)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun disableDnd() {
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }
}