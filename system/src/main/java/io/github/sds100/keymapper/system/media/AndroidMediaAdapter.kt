package io.github.sds100.keymapper.system.media

import android.content.ComponentName
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.MediaPlayer
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.view.KeyEvent
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.notifications.NotificationReceiver
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
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope,
) : MediaAdapter {
    companion object {
        /**
         * How many milliseconds to skip when seeking forward or backward.
         */
        private const val SEEK_AMOUNT = 30000L
    }

    private val mediaSessionManager: MediaSessionManager by lazy { ctx.getSystemService()!! }
    private val audioManager: AudioManager by lazy { ctx.getSystemService()!! }

    private val notificationListenerComponent by lazy {
        ComponentName(
            ctx,
            NotificationReceiver::class.java,
        )
    }

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
        val session = getPackageMediaSession(packageName) ?: return KMError.NoMediaSessions

        if (session.isPlaybackActionSupported(PlaybackState.ACTION_FAST_FORWARD)) {
            session.transportControls.fastForward()
            return Success(Unit)
        } else {
            return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, packageName)
        }
    }

    override fun rewind(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName) ?: return KMError.NoMediaSessions

        if (session.isPlaybackActionSupported(PlaybackState.ACTION_REWIND)) {
            session.transportControls.rewind()
            return Success(Unit)
        } else {
            return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_REWIND, packageName)
        }
    }

    override fun play(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName) ?: return KMError.NoMediaSessions

        if (session.isPlaybackActionSupported(PlaybackState.ACTION_PLAY)) {
            session.transportControls.play()
            return Success(Unit)
        } else {
            return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY, packageName)
        }
    }

    override fun pause(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName) ?: return KMError.NoMediaSessions

        if (session.isPlaybackActionSupported(PlaybackState.ACTION_PAUSE)) {
            session.transportControls.pause()
            return Success(Unit)
        } else {
            return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, packageName)
        }
    }

    override fun playPause(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName) ?: return KMError.NoMediaSessions

        if (session.isPlaybackActionSupported(PlaybackState.ACTION_PLAY_PAUSE)) {
            when (session.playbackState?.state) {
                PlaybackState.STATE_PLAYING -> session.transportControls.pause()
                PlaybackState.STATE_PAUSED -> session.transportControls.play()
                else -> {}
            }
            return Success(Unit)
        } else {
            return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, packageName)
        }
    }

    override fun previousTrack(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName) ?: return KMError.NoMediaSessions

        if (session.isPlaybackActionSupported(PlaybackState.ACTION_SKIP_TO_PREVIOUS)) {
            session.transportControls.skipToPrevious()
            return Success(Unit)
        } else {
            return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, packageName)
        }
    }

    override fun nextTrack(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName) ?: return KMError.NoMediaSessions

        if (session.isPlaybackActionSupported(PlaybackState.ACTION_SKIP_TO_NEXT)) {
            session.transportControls.skipToNext()
            return Success(Unit)
        } else {
            return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT, packageName)
        }
    }

    override fun stop(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName) ?: return KMError.NoMediaSessions

        if (session.isPlaybackActionSupported(PlaybackState.ACTION_STOP)) {
            session.transportControls.stop()
            return Success(Unit)
        } else {
            return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP, packageName)
        }
    }

    override fun stepForward(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName) ?: return KMError.NoMediaSessions

        if (session.isPlaybackActionSupported(PlaybackState.ACTION_SEEK_TO)) {
            val position = session.playbackState?.position ?: return KMError.NoMediaSessions
            session.transportControls.seekTo(position + SEEK_AMOUNT)
            return Success(Unit)
        } else {
            return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STEP_FORWARD, packageName)
        }
    }

    override fun stepBackward(packageName: String?): KMResult<*> {
        val session = getPackageMediaSession(packageName) ?: return KMError.NoMediaSessions

        if (session.isPlaybackActionSupported(PlaybackState.ACTION_SEEK_TO)) {
            val position = session.playbackState?.position ?: return KMError.NoMediaSessions
            session.transportControls.seekTo(max(0, position - SEEK_AMOUNT))
            return Success(Unit)
        } else {
            return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD, packageName)
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

    // Important! See issue #1971. Some apps do not expose media controls or say they support
    // media control actions but they still respond to key events.
    private fun sendMediaKeyEvent(keyCode: Int, packageName: String?): KMResult<*> {
        if (packageName == null) {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        } else {
            val session = getPackageMediaSession(packageName) ?: return KMError.NoMediaSessions
            session.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            session.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }

        return Success(Unit)
    }

    private fun MediaController.isPlaybackActionSupported(action: Long): Boolean =
        (playbackState?.actions ?: 0) and action != 0L

    private fun getPackageMediaSession(packageName: String?): MediaController? {
        val mediaSessions = mediaSessionManager.getActiveSessions(notificationListenerComponent)

        if (packageName == null) {
            return mediaSessions.firstOrNull()
        } else {
            for (session in mediaSessions) {
                if (session.packageName == packageName) {
                    return session
                }
            }
        }

        return null
    }
}
