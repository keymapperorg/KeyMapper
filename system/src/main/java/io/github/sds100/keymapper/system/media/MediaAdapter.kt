package io.github.sds100.keymapper.system.media

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.system.volume.VolumeStream
import kotlinx.coroutines.flow.Flow


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

    fun fastForward(packageName: String? = null): KMResult<*>
    fun rewind(packageName: String? = null): KMResult<*>
    fun play(packageName: String? = null): KMResult<*>
    fun pause(packageName: String? = null): KMResult<*>
    fun playPause(packageName: String? = null): KMResult<*>
    fun previousTrack(packageName: String? = null): KMResult<*>
    fun nextTrack(packageName: String? = null): KMResult<*>
    fun stop(packageName: String? = null): KMResult<*>
    fun stepForward(packageName: String? = null): KMResult<*>
    fun stepBackward(packageName: String? = null): KMResult<*>

    fun playFile(uri: String, stream: VolumeStream): KMResult<*>
    fun stopFileMedia(): KMResult<*>
}
