package io.github.sds100.keymapper.system.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.MediaPlayer
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.volume.VolumeStream
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class AndroidMediaAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
) : MediaAdapter {
    companion object {
        /**
         * How many milliseconds to skip when seeking forward or backward.
         */
        private const val SEEK_AMOUNT = 30000L
    }

    private val ctx = context.applicationContext

    private val audioManager: AudioManager by lazy { ctx.getSystemService()!! }

    private val activeMediaSessions: MutableStateFlow<List<MediaController>> =
        MutableStateFlow(emptyList())

    private val audioVolumeControlStreams: MutableStateFlow<Set<Int>> =
        MutableStateFlow(getActiveAudioVolumeStreams())

    private val audioPlaybackCallback =
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(
                configs: MutableList<AudioPlaybackConfiguration>?,
            ) {
                audioVolumeControlStreams.update { getActiveAudioVolumeStreams() }
            }
        }

    private var mediaPlayerLock = Any()
    private var mediaPlayer: MediaPlayer? = null

    init {
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

    override fun fastForward(packageName: String?): KMResult<*> {
        val controls = getPackageTransportControls(packageName)

        if (controls == null) {
            return KMError.NoMediaSessions
        } else {
            controls.fastForward()
            return Success(Unit)
        }
    }

    override fun rewind(packageName: String?): KMResult<*> {
        val controls = getPackageTransportControls(packageName)

        if (controls == null) {
            return KMError.NoMediaSessions
        } else {
            controls.rewind()
            return Success(Unit)
        }
    }

    override fun play(packageName: String?): KMResult<*> {
        val controls = getPackageTransportControls(packageName)

        if (controls == null) {
            return KMError.NoMediaSessions
        } else {
            controls.play()
            return Success(Unit)
        }
    }

    override fun pause(packageName: String?): KMResult<*> {
        val controls = getPackageTransportControls(packageName)

        if (controls == null) {
            return KMError.NoMediaSessions
        } else {
            controls.pause()
            return Success(Unit)
        }
    }

    override fun playPause(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName)

        if (session == null) {
            return KMError.NoMediaSessions
        } else {
            when (session.playbackState?.state) {
                PlaybackState.STATE_PLAYING -> session.transportControls.pause()
                PlaybackState.STATE_PAUSED -> session.transportControls.play()
                else -> {}
            }

            return Success(Unit)
        }
    }

    override fun previousTrack(packageName: String?): KMResult<*> {
        val controls = getPackageTransportControls(packageName)

        if (controls == null) {
            return KMError.NoMediaSessions
        } else {
            controls.skipToPrevious()
            return Success(Unit)
        }
    }

    override fun nextTrack(packageName: String?): KMResult<*> {
        val controls = getPackageTransportControls(packageName)

        if (controls == null) {
            return KMError.NoMediaSessions
        } else {
            controls.skipToNext()
            return Success(Unit)
        }
    }

    override fun stop(packageName: String?): KMResult<*> {
        val controls = getPackageTransportControls(packageName)

        if (controls == null) {
            return KMError.NoMediaSessions
        } else {
            controls.stop()
            return Success(Unit)
        }
    }

    override fun stepForward(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName)

        if (session == null) {
            return KMError.NoMediaSessions
        } else {
            val position = session.playbackState?.position ?: return KMError.NoMediaSessions
            session.transportControls.seekTo(position + SEEK_AMOUNT)

            return Success(Unit)
        }
    }

    override fun stepBackward(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName)

        if (session == null) {
            return KMError.NoMediaSessions
        } else {
            val position = session.playbackState?.position ?: return KMError.NoMediaSessions
            val newPosition = max(position - SEEK_AMOUNT, 0)
            session.transportControls.seekTo(newPosition)
            return Success(Unit)
        }
    }

    override fun stopFileMedia(): KMResult<*> {
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

    override fun getActiveAudioVolumeStreams(): Set<Int> {
        return audioManager.activePlaybackConfigurations
            .map { it.audioAttributes.volumeControlStream }
            .toSet()
    }

    override fun getActiveAudioVolumeStreamsFlow(): Flow<Set<Int>> {
        return audioVolumeControlStreams
    }

    override fun playFile(uri: String, stream: VolumeStream): KMResult<*> {
        try {
            synchronized(mediaPlayerLock) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }

            mediaPlayer = MediaPlayer().apply {
                val usage = when (stream) {
                    VolumeStream.ACCESSIBILITY -> AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY

                    else -> throw Exception(
                        "Don't know how to convert volume stream to audio usage attribute",
                    )
                }

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(usage)
                        .build(),
                )

                setDataSource(ctx, uri.toUri())

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
            return KMError.SourceFileNotFound(uri)
        } catch (e: Exception) {
            return KMError.Exception(e)
        }
    }

    fun onActiveMediaSessionChange(mediaSessions: List<MediaController>) {
        activeMediaSessions.update { mediaSessions }
    }

    private fun getPackageMediaSession(packageName: String?): MediaController? {
        if (packageName == null) {
            return activeMediaSessions.value.firstOrNull()
        } else {
            for (session in activeMediaSessions.value) {
                if (session.packageName == packageName) {
                    return session
                }
            }
        }

        return null
    }

    private fun getPackageTransportControls(
        packageName: String?,
    ): MediaController.TransportControls? {
        return getPackageMediaSession(packageName)?.transportControls
    }
}
