package io.github.sds100.keymapper.system.media

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.view.KeyEvent
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success

/**
 * Created by sds100 on 21/04/2021.
 */
class AndroidMediaAdapter(context: Context) : MediaAdapter {
    private val ctx = context.applicationContext

    private val audioManager: AudioManager by lazy { ctx.getSystemService()!! }

    private var activeMediaSessions: List<MediaController> = emptyList()

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
        return activeMediaSessions
            .filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            .map { it.packageName }
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