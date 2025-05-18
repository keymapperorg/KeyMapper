package io.github.sds100.keymapper.system.accessibility

import io.github.sds100.keymapper.base.actions.PerformActionsUseCase
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.detection.DetectKeyMapsUseCase
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityServiceController
import io.github.sds100.keymapper.reroutekeyevents.RerouteKeyEventsUseCase
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.base.utils.ServiceEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class AccessibilityServiceController(
    coroutineScope: CoroutineScope,
    accessibilityService: MyAccessibilityService,
    inputEvents: SharedFlow<ServiceEvent>,
    outputEvents: MutableSharedFlow<ServiceEvent>,
    detectConstraintsUseCase: DetectConstraintsUseCase,
    performActionsUseCase: PerformActionsUseCase,
    detectKeyMapsUseCase: DetectKeyMapsUseCase,
    fingerprintGesturesSupportedUseCase: FingerprintGesturesSupportedUseCase,
    rerouteKeyEventsUseCase: RerouteKeyEventsUseCase,
    pauseKeyMapsUseCase: PauseKeyMapsUseCase,
    devicesAdapter: DevicesAdapter,
    suAdapter: SuAdapter,
    inputMethodAdapter: InputMethodAdapter,
    settingsRepository: PreferenceRepository,
    nodeRepository: AccessibilityNodeRepository,
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
    pauseKeyMapsUseCase,
    devicesAdapter,
    suAdapter,
    inputMethodAdapter,
    settingsRepository,
    nodeRepository,
)
