package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.mappings.fingerprintmaps.DefaultFingerprintMapOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Created by sds100 on 15/05/2021.
 */
class FakeSimpleMappingController(
    coroutineScope: CoroutineScope,
    detectMappingUseCase: DetectMappingUseCase,
    performActionsUseCase: PerformActionsUseCase,
    detectConstraintsUseCase: DetectConstraintsUseCase
) : SimpleMappingController(coroutineScope, detectMappingUseCase, performActionsUseCase, detectConstraintsUseCase) {
    override val defaultOptions: MutableStateFlow<DefaultMappingOptions> = MutableStateFlow(
        DefaultFingerprintMapOptions(100, false)
    )
}