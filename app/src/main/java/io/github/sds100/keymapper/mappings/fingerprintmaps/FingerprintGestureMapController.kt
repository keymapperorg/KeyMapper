package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.mappings.SimpleMappingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 11/12/20.
 */
class FingerprintGestureMapController(
    coroutineScope: CoroutineScope,
    private val detectMappingUseCase: DetectFingerprintMapsUseCase,
    private val performActionsUseCase: PerformActionsUseCase,
    private val detectConstraintsUseCase: DetectConstraintsUseCase
) : SimpleMappingController(
    coroutineScope,
    detectMappingUseCase,
    performActionsUseCase,
    detectConstraintsUseCase
) {
    private var fingerprintMaps: FingerprintMapGroup? = null

    init {
        coroutineScope.launch {
            detectMappingUseCase.fingerprintMaps.collectLatest { fingerprintMaps ->
                reset()

                this@FingerprintGestureMapController.fingerprintMaps = fingerprintMaps
            }
        }
    }

    fun onGesture(id: FingerprintMapId) {
        val fingerprintMap = fingerprintMaps?.get(id) ?: return

        onDetected(id.toString(), fingerprintMap)
    }
}