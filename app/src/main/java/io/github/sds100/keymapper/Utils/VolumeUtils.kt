package io.github.sds100.keymapper.Utils

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import io.github.sds100.keymapper.R
import org.jetbrains.anko.selector

/**
 * Created by sds100 on 21/10/2018.
 */

object VolumeUtils {
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

    private val STREAM_TYPE_LABEL_MAP = sequence {
        yield(AudioManager.STREAM_ALARM to R.string.stream_alarm)
        yield(AudioManager.STREAM_DTMF to R.string.stream_dtmf)
        yield(AudioManager.STREAM_MUSIC to R.string.stream_music)
        yield(AudioManager.STREAM_NOTIFICATION to R.string.stream_notification)
        yield(AudioManager.STREAM_RING to R.string.stream_ring)
        yield(AudioManager.STREAM_SYSTEM to R.string.stream_system)
        yield(AudioManager.STREAM_VOICE_CALL to R.string.stream_voice_call)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            yield(AudioManager.STREAM_ACCESSIBILITY to R.string.stream_accessibility)
        }

    }.toMap()

    /**
     * @param adjustMode must be one of the AudioManager.ADJUST... values
     */
    fun adjustVolume(ctx: Context,
                     @AdjustMode adjustMode: Int,
                     showVolumeUi: Boolean = false) {
        val audioManager = ctx.applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager

        val flag = if (showVolumeUi) {
            AudioManager.FLAG_SHOW_UI
        } else {
            0
        }

        audioManager.adjustVolume(adjustMode, flag)
    }

    fun adjustSpecificStream(ctx: Context,
                             @AdjustMode adjustMode: Int,
                             showVolumeUi: Boolean,
                             @StreamType streamType: Int) {
        val audioManager = ctx.applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager

        val flag = if (showVolumeUi) {
            AudioManager.FLAG_SHOW_UI
        } else {
            0
        }

        audioManager.adjustStreamVolume(streamType, adjustMode, flag)
    }

    fun showStreamPickerDialog(ctx: Context,
                               onPosClick: (streamType: Int) -> Unit) {

        //get all the strings from resources
        val labels = sequence {
            STREAM_TYPE_LABEL_MAP.forEach { yield(ctx.str(it.value)) }
        }.toList()

        ctx.selector(
                title = ctx.str(R.string.dialog_title_pick_stream),
                items = labels
        ) { _, which ->
            //"which" is the index.
            onPosClick(STREAM_TYPE_LABEL_MAP.toList()[which].first)
        }
    }

    @StringRes
    fun getStreamLabel(@StreamType streamType: Int) = STREAM_TYPE_LABEL_MAP[streamType]!!
}