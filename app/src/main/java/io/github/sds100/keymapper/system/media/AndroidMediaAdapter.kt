package io.github.sds100.keymapper.system.media

import android.content.ComponentName
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.view.KeyEvent
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.system.notifications.NotificationReceiver
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.volume.VolumeStream
import io.github.sds100.keymapper.util.*
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 21/04/2021.
 */
class AndroidMediaAdapter(
    context: Context,
    private val permissionAdapter: PermissionAdapter
) : MediaAdapter {
    private val ctx = context.applicationContext

    private val mediaSessionManager: MediaSessionManager by lazy { ctx.getSystemService()!! }
    private val audioManager: AudioManager by lazy { ctx.getSystemService()!! }

    private var mediaPlayerLock = Any()
    private var mediaPlayer: MediaPlayer? = null

    override fun fastForward(packageName: String?): Result<*> {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, packageName)
    }

    override fun rewind(packageName: String?): Result<*> {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_REWIND, packageName)
    }

    override fun play(packageName: String?): Result<*> {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY, packageName)
    }

    override fun pause(packageName: String?): Result<*> {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, packageName)
    }

    override fun playPause(packageName: String?): Result<*> {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, packageName)
    }

    override fun previousTrack(packageName: String?): Result<*> {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, packageName)
    }

    override fun nextTrack(packageName: String?): Result<*> {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT, packageName)
    }

    override fun getPackagesPlayingMedia(): List<String> {
        val activeMediaSessions = getActiveMediaSessions().valueOrNull() ?: return emptyList()

        return activeMediaSessions
            .filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            .map { it.packageName }
    }

    override fun playSoundFile(uri: String, stream: VolumeStream): Result<*> {
        try {
            synchronized(mediaPlayerLock) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }

            mediaPlayer = MediaPlayer().apply {
                val usage = when (stream) {
                    VolumeStream.ACCESSIBILITY -> AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                    else -> throw Exception("Don't know how to convert volume stream to audio usage attribute")
                }

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(usage)
                        .build()
                )

                setDataSource(ctx, Uri.parse(uri))

                setOnCompletionListener {
                    synchronized(mediaPlayerLock) {
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                }

                prepare()
                start()
            }

            return Success(Unit)
        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    private fun sendMediaKeyEvent(keyCode: Int, packageName: String?): Result<*> {
        if (packageName == null) {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            return Success(Unit)
        } else {
            return getActiveMediaSessions().onSuccess { mediaSessions ->
                for (session in mediaSessions) {
                    if (session.packageName == packageName) {
                        session.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                        session.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                        break
                    }
                }
            }
        }
    }

    private fun getActiveMediaSessions(): Result<List<MediaController>> {
        if (!permissionAdapter.isGranted(Permission.NOTIFICATION_LISTENER)) {
            return Error.PermissionDenied(Permission.NOTIFICATION_LISTENER)
        }

        val component = ComponentName(ctx, NotificationReceiver::class.java)

        return Success(mediaSessionManager.getActiveSessions(component))
    }
}