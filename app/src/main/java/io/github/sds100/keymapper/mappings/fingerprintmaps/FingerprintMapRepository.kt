package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntity
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntityGroup
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 24/01/21.
 */
interface FingerprintMapRepository {
    val fingerprintMaps: Flow<FingerprintMapEntityGroup>
    val requestBackup: Flow<FingerprintMapEntityGroup>

    fun enableFingerprintMap(id: String)
    fun disableFingerprintMap(id: String)
    fun update(id: String, fingerprintMap: FingerprintMapEntity)

    suspend fun restore(id: String, fingerprintMapJson: String)

    fun reset()
}