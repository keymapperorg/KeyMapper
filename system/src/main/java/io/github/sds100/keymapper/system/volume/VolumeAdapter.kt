package io.github.sds100.keymapper.system.volume

import io.github.sds100.keymapper.common.utils.KMResult


interface VolumeAdapter {
    val ringerMode: RingerMode

    fun raiseVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): KMResult<*>
    fun lowerVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): KMResult<*>
    fun muteVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): KMResult<*>
    fun unmuteVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): KMResult<*>
    fun toggleMuteVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): KMResult<*>
    fun showVolumeUi(): KMResult<*>
    fun setRingerMode(mode: RingerMode): KMResult<*>

    fun isDndEnabled(): Boolean
    fun enableDndMode(dndMode: DndMode): KMResult<*>
    fun disableDndMode(): KMResult<*>
}
