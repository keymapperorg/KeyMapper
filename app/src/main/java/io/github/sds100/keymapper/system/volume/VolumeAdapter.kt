package io.github.sds100.keymapper.system.volume

import io.github.sds100.keymapper.common.result.Result

/**
 * Created by sds100 on 20/04/2021.
 */
interface VolumeAdapter {
    val ringerMode: RingerMode

    fun raiseVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): Result<*>
    fun lowerVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): Result<*>
    fun muteVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): Result<*>
    fun unmuteVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): Result<*>
    fun toggleMuteVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): Result<*>
    fun showVolumeUi(): Result<*>
    fun setRingerMode(mode: RingerMode): Result<*>

    fun isDndEnabled(): Boolean
    fun enableDndMode(dndMode: DndMode): Result<*>
    fun disableDndMode(): Result<*>
}
