package io.github.sds100.keymapper.system.accessibility

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.sds100.keymapper.base.actions.PerformActionsUseCaseImpl
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCaseImpl
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.detection.DetectKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.reroutekeyevents.RerouteKeyEventsController
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityNodeRecorder
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityServiceController
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.root.SuAdapter

class AccessibilityServiceController @AssistedInject constructor(
    @Assisted
    private val service: MyAccessibilityService,
    rerouteKeyEventsControllerFactory: RerouteKeyEventsController.Factory,
    accessibilityNodeRecorderFactory: AccessibilityNodeRecorder.Factory,
    performActionsUseCaseFactory: PerformActionsUseCaseImpl.Factory,
    detectKeyMapsUseCaseFactory: DetectKeyMapsUseCaseImpl.Factory,
    detectConstraintsUseCaseFactory: DetectConstraintsUseCaseImpl.Factory,
    fingerprintGesturesSupported: FingerprintGesturesSupportedUseCase,
    pauseKeyMapsUseCase: PauseKeyMapsUseCase,
    devicesAdapter: DevicesAdapter,
    suAdapter: SuAdapter,
    settingsRepository: PreferenceRepository,
) : BaseAccessibilityServiceController(
    service = service,
    rerouteKeyEventsControllerFactory = rerouteKeyEventsControllerFactory,
    accessibilityNodeRecorderFactory = accessibilityNodeRecorderFactory,
    performActionsUseCaseFactory = performActionsUseCaseFactory,
    detectKeyMapsUseCaseFactory = detectKeyMapsUseCaseFactory,
    detectConstraintsUseCaseFactory = detectConstraintsUseCaseFactory,
    fingerprintGesturesSupported = fingerprintGesturesSupported,
    pauseKeyMapsUseCase = pauseKeyMapsUseCase,
    devicesAdapter = devicesAdapter,
    suAdapter = suAdapter,
    settingsRepository = settingsRepository,
) {
    @AssistedFactory
    interface Factory {
        fun create(service: MyAccessibilityService): AccessibilityServiceController
    }
}
