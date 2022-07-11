package io.github.sds100.keymapper.mappings.detection

import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMap
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Created by sds100 on 11/12/20.
 */
class FingerprintGestureMapController(
    coroutineScope: CoroutineScope,
    detectMappingUseCase: DetectFingerprintMapsUseCase,
    performActionsUseCase: PerformActionsUseCase,
    detectConstraintsUseCase: DetectConstraintsUseCase
) : SimpleMappingController(
    coroutineScope,
    detectMappingUseCase,
    performActionsUseCase,
    detectConstraintsUseCase
) {
    private var fingerprintMaps: List<FingerprintMap>? = null

    init {
        coroutineScope.launch {
            detectMappingUseCase.fingerprintMaps.collectLatest { fingerprintMaps ->
                reset()

                this@FingerprintGestureMapController.fingerprintMaps = fingerprintMaps
            }
        }
    }

    fun onGesture(id: FingerprintMapId) {
        Timber.d("Fingerprint gesture: $id")
        val fingerprintMap = fingerprintMaps?.find { it.id == id } ?: return

        onDetected(id.toString(), fingerprintMap)
    }
}