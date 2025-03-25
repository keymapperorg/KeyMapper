package io.github.sds100.keymapper.system.accessibility

import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.mappings.keymaps.detection.DetectKeyMapsUseCase
import io.github.sds100.keymapper.reroutekeyevents.RerouteKeyEventsUseCase
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.ServiceEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class AccessibilityServiceController(
    coroutineScope: CoroutineScope,
    accessibilityService: IAccessibilityService,
    inputEvents: SharedFlow<ServiceEvent>,
    outputEvents: MutableSharedFlow<ServiceEvent>,
    detectConstraintsUseCase: DetectConstraintsUseCase,
    performActionsUseCase: PerformActionsUseCase,
    detectKeyMapsUseCase: DetectKeyMapsUseCase,
    fingerprintGesturesSupportedUseCase: FingerprintGesturesSupportedUseCase,
    rerouteKeyEventsUseCase: RerouteKeyEventsUseCase,
    pauseMappingsUseCase: PauseMappingsUseCase,
    devicesAdapter: DevicesAdapter,
    suAdapter: SuAdapter,
    inputMethodAdapter: InputMethodAdapter,
    settingsRepository: PreferenceRepository,
) : BaseAccessibilityServiceController(
    coroutineScope,
    accessibilityService,
    inputEvents,
    outputEvents,
    detectConstraintsUseCase,
    performActionsUseCase,
    detectKeyMapsUseCase,
    fingerprintGesturesSupportedUseCase,
    rerouteKeyEventsUseCase,
    pauseMappingsUseCase,
    devicesAdapter,
    suAdapter,
    inputMethodAdapter,
    settingsRepository,
)
