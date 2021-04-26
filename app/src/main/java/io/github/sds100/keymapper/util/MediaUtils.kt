package io.github.sds100.keymapper.util

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresApi


/**
 * Created by sds100 on 05/11/2018.
 */
object MediaUtils {
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun playMedia(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PLAY)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun pauseMediaPlayback(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PAUSE)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun playPauseMediaPlayback(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun nextTrack(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_NEXT)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun previousTrack(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun fastForward(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun rewind(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_REWIND)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun sendMediaKeyEvent(ctx: Context, keyEvent: Int) {
        //simulates media key being pressed.
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEvent))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEvent))
    }

    fun playNotification(context: Context?) {
        val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(context, notification)
        r.play()
    }
}