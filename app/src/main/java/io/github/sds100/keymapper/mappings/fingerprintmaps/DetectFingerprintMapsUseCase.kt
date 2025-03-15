package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.mappings.DetectMappingUseCase
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * Created by sds100 on 17/04/2021.
 */

class DetectFingerprintMapsUseCaseImpl(
    private val repository: FingerprintMapRepository,
    private val areSupportedUseCase: FingerprintGesturesSupportedUseCase,
    detectMappingUseCase: DetectMappingUseCase,
) : DetectFingerprintMapsUseCase,
    DetectMappingUseCase by detectMappingUseCase {
    override val fingerprintMaps: Flow<List<FingerprintMap>> =
        repository.fingerprintMapList
            .mapNotNull { state ->
                if (state is State.Data) {
                    state.data
                } else {
                    null
                }
            }
            .map { entityList -> entityList.map { FingerprintMapEntityMapper.fromEntity(it) } }
            .flowOn(Dispatchers.Default)

    override val isSupported: Flow<Boolean?> = areSupportedUseCase.isSupported

    override fun setSupported(supported: Boolean) {
        areSupportedUseCase.setSupported(supported)
    }
}

interface DetectFingerprintMapsUseCase : DetectMappingUseCase {
    val fingerprintMaps: Flow<List<FingerprintMap>>

    val isSupported: Flow<Boolean?>
    fun setSupported(supported: Boolean)
}
