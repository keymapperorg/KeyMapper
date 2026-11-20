package io.github.sds100.keymapper.system.volume

import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow

interface VolumeAdapter {
    val ringerMode: RingerMode
    val ringerModeFlow: Flow<RingerMode>

    /**
     * The current Do Not Disturb mode or null if Do Not Disturb is off.
     */
    val dndMode: DndMode?
    val dndModeFlow: Flow<DndMode?>

    fun raiseVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): KMResult<*>
    fun lowerVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): KMResult<*>
    fun muteVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): KMResult<*>
    fun unmuteVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): KMResult<*>
    fun toggleMuteVolume(stream: VolumeStream? = null, showVolumeUi: Boolean): KMResult<*>
    fun showVolumeUi(): KMResult<*>
    fun setRingerMode(mode: RingerMode): KMResult<*>

    val isMicrophoneMuted: Boolean
    fun muteMicrophone(): KMResult<*>
    fun unmuteMicrophone(): KMResult<*>

    fun isDndEnabled(): Boolean
    fun enableDndMode(dndMode: DndMode): KMResult<*>
    fun disableDndMode(): KMResult<*>
}
