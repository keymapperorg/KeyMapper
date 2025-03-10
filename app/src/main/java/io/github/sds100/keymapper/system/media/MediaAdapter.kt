package io.github.sds100.keymapper.system.media

import io.github.sds100.keymapper.system.volume.VolumeStream
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 21/04/2021.
 */
interface MediaAdapter {

    fun getPackagesPlayingMedia(): List<String>
    fun getPackagesPlayingMediaFlow(): Flow<List<String>>
    fun fastForward(packageName: String? = null): Result<*>
    fun rewind(packageName: String? = null): Result<*>
    fun play(packageName: String? = null): Result<*>
    fun pause(packageName: String? = null): Result<*>
    fun playPause(packageName: String? = null): Result<*>
    fun previousTrack(packageName: String? = null): Result<*>
    fun nextTrack(packageName: String? = null): Result<*>

    fun playSoundFile(uri: String, stream: VolumeStream): Result<*>
    fun stopMedia(): Result<*>
}
