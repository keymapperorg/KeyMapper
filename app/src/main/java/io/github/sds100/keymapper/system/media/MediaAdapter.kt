package io.github.sds100.keymapper.system.media

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.system.volume.VolumeStream
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 21/04/2021.
 */
interface MediaAdapter {

    /**
     * This gets the package names of the apps that have registered playback notifications and
     * are currently playing. It does NOT return apps that are just playing sounds without
     * registering a notification, like Tiktok etc. Use getActiveAudioContentTypes() to detect
     * whether media is possibly playing
     */
    fun getActiveMediaSessionPackages(): List<String>
    fun getActiveMediaSessionPackagesFlow(): Flow<List<String>>

    /**
     * Get the different types of content currently playing. The types can be found in
     * the AudioAttributes class.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getActiveAudioVolumeStreams(): Set<Int>
    fun getActiveAudioVolumeStreamsFlow(): Flow<Set<Int>>

    fun fastForward(packageName: String? = null): Result<*>
    fun rewind(packageName: String? = null): Result<*>
    fun play(packageName: String? = null): Result<*>
    fun pause(packageName: String? = null): Result<*>
    fun playPause(packageName: String? = null): Result<*>
    fun previousTrack(packageName: String? = null): Result<*>
    fun nextTrack(packageName: String? = null): Result<*>
    fun stop(packageName: String? = null): Result<*>
    fun stepForward(packageName: String? = null): Result<*>
    fun stepBackward(packageName: String? = null): Result<*>

    fun playFile(uri: String, stream: VolumeStream): Result<*>
    fun stopFileMedia(): Result<*>
}
