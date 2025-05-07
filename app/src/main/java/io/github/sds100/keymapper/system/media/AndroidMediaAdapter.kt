package io.github.sds100.keymapper.system.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileNotFoundException

/**
 * Created by sds100 on 21/04/2021.
 */
class AndroidMediaAdapter(context: Context, coroutineScope: CoroutineScope) : MediaAdapter {
    private val ctx = context.applicationContext

    private val audioManager: AudioManager by lazy { ctx.getSystemService()!! }

    private val activeMediaSessions: MutableStateFlow<List<MediaController>> =
        MutableStateFlow(emptyList())

    private val audioVolumeControlStreams: MutableStateFlow<Set<Int>> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MutableStateFlow(getActiveAudioVolumeStreams())
        } else {
            MutableStateFlow(emptySet())
        }

    private val audioPlaybackCallback by lazy {
        @RequiresApi(Build.VERSION_CODES.O)
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                audioVolumeControlStreams.update { getActiveAudioVolumeStreams() }
            }
        }
    }

    private var mediaPlayerLock = Any()
    private var mediaPlayer: MediaPlayer? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            coroutineScope.launch {
                audioVolumeControlStreams.subscriptionCount.collect { count ->
                    if (count == 0) {
                        audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback)
                    } else {
                        audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, null)
                    }
                }
            }
        }
    }

    override fun fastForward(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, packageName)

    override fun rewind(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_REWIND, packageName)

    override fun play(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY, packageName)

    override fun pause(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, packageName)

    override fun playPause(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, packageName)

    override fun previousTrack(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, packageName)

    override fun nextTrack(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT, packageName)

    override fun stop(packageName: String?): Result<*> = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP, packageName)

    override fun stopFileMedia(): Result<*> {
        synchronized(mediaPlayerLock) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }

        return Success(Unit)
    }

    override fun getActiveMediaSessionPackages(): List<String> {
        return activeMediaSessions.value
            .filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            .map { it.packageName }
    }

    override fun getActiveMediaSessionPackagesFlow() = activeMediaSessions
        .map { list ->
            list.filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                .map { it.packageName }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getActiveAudioVolumeStreams(): Set<Int> {
        return audioManager.activePlaybackConfigurations
            .map { it.audioAttributes.volumeControlStream }
            .toSet()
    }

    override fun getActiveAudioVolumeStreamsFlow(): Flow<Set<Int>> {
        return audioVolumeControlStreams
    }

    override fun playFile(uri: String, stream: VolumeStream): Result<*> {
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

    fun onActiveMediaSessionChange(mediaSessions: List<MediaController>) {
        activeMediaSessions.update { mediaSessions }
    }

    private fun sendMediaKeyEvent(keyCode: Int, packageName: String?): Result<*> {
        if (packageName == null) {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        } else {
            for (session in activeMediaSessions.value) {
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
