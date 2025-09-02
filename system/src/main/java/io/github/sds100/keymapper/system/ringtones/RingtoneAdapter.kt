package io.github.sds100.keymapper.system.ringtones

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidRingtoneAdapter @Inject constructor(@ApplicationContext private val ctx: Context) : RingtoneAdapter {

    private val ringtoneManager: RingtoneManager by lazy {
        RingtoneManager(ctx).apply {
            setType(RingtoneManager.TYPE_ALL)
            stopPreviousRingtone = true
        }
    }

    private val lock = Any()
    private var playingRingtone: Ringtone? = null

    override fun getLabel(uri: String): KMResult<String> {
        val ringtone = getRingtone(uri)

        if (ringtone == null) {
            return KMError.CantFindSoundFile
        }

        return Success(ringtone.getTitle(ctx))
    }

    override fun exists(uri: String): Boolean {
        return getRingtone(uri) != null
    }

    override fun play(uri: String): KMResult<Unit> {
        val ringtone = getRingtone(uri)

        if (ringtone == null) {
            return KMError.CantFindSoundFile
        } else {
            ringtoneManager.stopPreviousRingtone()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = false
            }

            synchronized(lock) {
                playingRingtone?.stop()
                playingRingtone = ringtone
                ringtone.play()
            }

            return Success(Unit)
        }
    }

    override fun stopPlaying() {
        ringtoneManager.stopPreviousRingtone()

        synchronized(lock) {
            playingRingtone?.stop()
            playingRingtone = null
        }
    }

    private fun getRingtone(uri: String): Ringtone? {
        return RingtoneManager.getRingtone(ctx, uri.toUri())
    }
}

interface RingtoneAdapter {
    fun getLabel(uri: String): KMResult<String>
    fun exists(uri: String): Boolean
    fun play(uri: String): KMResult<Unit>
    fun stopPlaying()
}
