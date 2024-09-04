package io.github.sds100.keymapper.mappings.fingerprintmaps

import android.os.Build
import io.github.sds100.keymapper.backup.BackupManager
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.DisplaySimpleMappingUseCase
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * Created by sds100 on 16/04/2021.
 */

class ListFingerprintMapsUseCaseImpl(
    private val fingerprintMapRepository: FingerprintMapRepository,
    private val backupManager: BackupManager,
    private val preferenceRepository: PreferenceRepository,
    displaySimpleMappingUseCase: DisplaySimpleMappingUseCase,
) : ListFingerprintMapsUseCase,
    DisplaySimpleMappingUseCase by displaySimpleMappingUseCase {

    override val fingerprintMaps: Flow<List<FingerprintMap>> =
        fingerprintMapRepository.fingerprintMapList
            .mapNotNull { state ->
                if (state is State.Data) {
                    state.data
                } else {
                    null
                }
            }
            .map { entityList -> entityList.map { FingerprintMapEntityMapper.fromEntity(it) } }
            .flowOn(Dispatchers.Default)

    override val showFingerprintMaps: Flow<Boolean> =
        preferenceRepository.get(Keys.fingerprintGesturesAvailable).map {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@map false

            it ?: false
        }

    override fun resetFingerprintMaps() {
        fingerprintMapRepository.reset()
    }

    override fun enableFingerprintMap(id: FingerprintMapId) {
        val entityId = FingerprintMapIdEntityMapper.toEntity(id)
        fingerprintMapRepository.enableFingerprintMap(entityId)
    }

    override fun disableFingerprintMap(id: FingerprintMapId) {
        val entityId = FingerprintMapIdEntityMapper.toEntity(id)
        fingerprintMapRepository.disableFingerprintMap(entityId)
    }

    override suspend fun backupFingerprintMaps(uri: String): Result<String> =
        backupManager.backupFingerprintMaps(uri)
}

interface ListFingerprintMapsUseCase : DisplaySimpleMappingUseCase {
    val fingerprintMaps: Flow<List<FingerprintMap>>
    val showFingerprintMaps: Flow<Boolean>

    fun enableFingerprintMap(id: FingerprintMapId)
    fun disableFingerprintMap(id: FingerprintMapId)
    fun resetFingerprintMaps()
    suspend fun backupFingerprintMaps(uri: String): Result<String>
}
