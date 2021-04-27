package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.mappings.DetectMappingUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 17/04/2021.
 */

class DetectFingerprintMapsUseCaseImpl(
    private val repository: FingerprintMapRepository,
    private val areSupportedUseCase: AreFingerprintGesturesSupportedUseCase,
    detectMappingUseCase: DetectMappingUseCase
) : DetectFingerprintMapsUseCase, DetectMappingUseCase by detectMappingUseCase {
    override val fingerprintMaps: Flow<FingerprintMapGroup> =
        repository.fingerprintMaps
            .map { entityGroup ->
                FingerprintMapGroup(
                    swipeDown = FingerprintMapEntityMapper.fromEntity(entityGroup.swipeDown),
                    swipeUp = FingerprintMapEntityMapper.fromEntity(entityGroup.swipeUp),
                    swipeLeft = FingerprintMapEntityMapper.fromEntity(entityGroup.swipeLeft),
                    swipeRight = FingerprintMapEntityMapper.fromEntity(entityGroup.swipeRight),
                )
            }.flowOn(Dispatchers.Default)


    override val isSupported: Flow<Boolean?> = areSupportedUseCase.isSupported

    override fun setSupported(supported: Boolean) {
        areSupportedUseCase.setSupported(supported)
    }
}

interface DetectFingerprintMapsUseCase : DetectMappingUseCase {
    val fingerprintMaps: Flow<FingerprintMapGroup>

    val isSupported: Flow<Boolean?>
    fun setSupported(supported: Boolean)
}