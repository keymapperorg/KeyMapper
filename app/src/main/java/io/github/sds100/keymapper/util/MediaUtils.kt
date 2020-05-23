package io.github.sds100.keymapper.util

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

/**
 * Created by sds100 on 05/11/2018.
 */
object MediaUtils {
    fun playMedia(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PLAY)
    fun pauseMediaPlayback(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PAUSE)
    fun playPauseMediaPlayback(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun nextTrack(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previousTrack(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    fun fastForward(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
    fun rewind(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_REWIND)

    private fun sendMediaKeyEvent(ctx: Context, keyEvent: Int) {
        //simulates media key being pressed.
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEvent))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEvent))
    }
}