package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.mappings.detection.PerformActionsUseCase
import io.github.sds100.keymapper.mappings.detection.DetectConstraintsUseCase
import io.github.sds100.keymapper.mappings.detection.DetectMappingUseCase
import io.github.sds100.keymapper.mappings.detection.SimpleMappingController
import kotlinx.coroutines.CoroutineScope

/**
 * Created by sds100 on 15/05/2021.
 */
class FakeSimpleMappingController(
    coroutineScope: CoroutineScope,
    detectMappingUseCase: DetectMappingUseCase,
    performActionsUseCase: PerformActionsUseCase,
    detectConstraintsUseCase: DetectConstraintsUseCase
) : SimpleMappingController(coroutineScope, detectMappingUseCase, performActionsUseCase, detectConstraintsUseCase)