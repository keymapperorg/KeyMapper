package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.data.entities.FingerprintMapEntity
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 24/01/21.
 */
interface FingerprintMapRepository {
    val fingerprintMapList: Flow<State<List<FingerprintMapEntity>>>
    val requestBackup: Flow<List<FingerprintMapEntity>>

    suspend fun get(id: Int): FingerprintMapEntity
    fun enableFingerprintMap(id: Int)
    fun disableFingerprintMap(id: Int)
    fun update(vararg fingerprintMap: FingerprintMapEntity)

    fun reset()
}
