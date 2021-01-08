package io.github.sds100.keymapper.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.service.NotificationReceiver
import io.github.sds100.keymapper.util.result.*
import splitties.systemservices.mediaSessionManager

/**
 * Created by sds100 on 05/11/2018.
 */
object MediaUtils {
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun playMedia(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PLAY)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun playMediaForPackage(ctx: Context, packageName: String
    ) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PLAY, packageName)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun pauseMediaPlayback(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PAUSE)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun pauseMediaForPackage(ctx: Context, packageName: String
    ) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PAUSE, packageName)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun playPauseMediaPlayback(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun playPauseMediaPlaybackForPackage(ctx: Context, packageName: String
    ) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, packageName)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun nextTrack(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_NEXT)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun nextTrackForPackage(ctx: Context, packageName: String
    ) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_NEXT, packageName)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun previousTrack(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun previousTrackForPackage(ctx: Context, packageName: String
    ) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS, packageName)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun fastForward(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun fastForwardForPackage(ctx: Context, packageName: String
    ) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, packageName)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun rewind(ctx: Context) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_REWIND)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun rewindForPackage(ctx: Context, packageName: String
    ) = sendMediaKeyEvent(ctx, KeyEvent.KEYCODE_MEDIA_REWIND, packageName)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun sendMediaKeyEvent(ctx: Context, keyCode: Int) {
        //simulates media key being pressed.
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun sendMediaKeyEvent(ctx: Context, keyCode: Int, packageName: String) {
        getActiveMediaSessions(ctx).onSuccess { mediaSessions ->
            for (session in mediaSessions) {
                if (session.packageName == packageName) {
                    session.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                    session.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                    break
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun highestPriorityPackagePlayingMedia(ctx: Context): Result<String> {
        return getActiveMediaSessions(ctx) then { mediaSessions ->
            Success(mediaSessions.map { it.packageName })
        } then {
            val packageName = it.elementAtOrNull(0)

            if (packageName == null) {
                NoMediaSessions()
            } else {
                Success(packageName)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getActiveMediaSessions(ctx: Context): Result<List<MediaController>> {
        if (PermissionUtils.isPermissionGranted(
                Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            val component = ComponentName(ctx, NotificationReceiver::class.java)

            return Success(mediaSessionManager.getActiveSessions(component))
        }

        return PermissionDenied(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)
    }
}