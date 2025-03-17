package io.github.sds100.keymapper.system.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.system.volume.VolumeStream
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import java.io.FileNotFoundException

/**
 * Created by sds100 on 21/04/2021.
 */
class AndroidMediaAdapter(context: Context) : MediaAdapter {
    private val ctx = context.applicationContext

    private val audioManager: AudioManager by lazy { ctx.getSystemService()!! }

    private var activeMediaSessions: List<MediaController> = emptyList()

    private var mediaPlayerLock = Any()
    private var mediaPlayer: MediaPlayer? = null

    override fun fastForward(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, packageName)

    override fun rewind(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_REWIND, packageName)

    override fun play(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY, packageName)

    override fun pause(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, packageName)

    override fun playPause(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, packageName)

    override fun previousTrack(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, packageName)

    override fun nextTrack(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT, packageName)

    override fun getActiveMediaSessionPackages(): List<String> {
        return activeMediaSessions
            .filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            .map { it.packageName }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getActiveAudioVolumeStreams(): Set<Int> {
        return audioManager.activePlaybackConfigurations
            .map { it.audioAttributes.volumeControlStream }
            .toSet()
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
                        .build(),
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
        } catch (e: FileNotFoundException) {
            return Error.SourceFileNotFound(uri)
        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    override fun stopMedia(): Result<*> {
        synchronized(mediaPlayerLock) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }

        return Success(Unit)
    }

    fun onActiveMediaSessionChange(mediaSessions: List<MediaController>) {
        activeMediaSessions = mediaSessions
    }

    private fun sendMediaKeyEvent(keyCode: Int, packageName: String?): Result<*> {
        if (packageName == null) {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        } else {
            for (session in activeMediaSessions) {
                if (session.packageName == packageName) {
                    session.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                    session.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                    break
                }
            }
        }

        return Success(Unit)
    }
}
