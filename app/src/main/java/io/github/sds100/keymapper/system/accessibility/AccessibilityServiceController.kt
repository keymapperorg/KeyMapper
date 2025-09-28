package io.github.sds100.keymapper.system.accessibility

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.sds100.keymapper.base.actions.PerformActionsUseCaseImpl
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCaseImpl
import io.github.sds100.keymapper.base.detection.DetectKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.promode.SystemBridgeSetupAssistantController
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityNodeRecorder
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityServiceController
import io.github.sds100.keymapper.base.system.inputmethod.AutoSwitchImeController
import io.github.sds100.keymapper.base.trigger.RecordTriggerController
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.inputmethod.KeyEventRelayServiceWrapper

class AccessibilityServiceController @AssistedInject constructor(
    @Assisted
    private val service: MyAccessibilityService,
    accessibilityNodeRecorderFactory: AccessibilityNodeRecorder.Factory,
    performActionsUseCaseFactory: PerformActionsUseCaseImpl.Factory,
    detectKeyMapsUseCaseFactory: DetectKeyMapsUseCaseImpl.Factory,
    detectConstraintsUseCaseFactory: DetectConstraintsUseCaseImpl.Factory,
    fingerprintGesturesSupported: FingerprintGesturesSupportedUseCase,
    pauseKeyMapsUseCase: PauseKeyMapsUseCase,
    settingsRepository: PreferenceRepository,
    keyEventRelayServiceWrapper: KeyEventRelayServiceWrapper,
    inputEventHub: InputEventHub,
    recordTriggerController: RecordTriggerController,
    setupAssistantControllerFactory: SystemBridgeSetupAssistantController.Factory,
    autoSwitchImeControllerFactory: AutoSwitchImeController.Factory,
) : BaseAccessibilityServiceController(
    service = service,
    accessibilityNodeRecorderFactory = accessibilityNodeRecorderFactory,
    performActionsUseCaseFactory = performActionsUseCaseFactory,
    detectKeyMapsUseCaseFactory = detectKeyMapsUseCaseFactory,
    detectConstraintsUseCaseFactory = detectConstraintsUseCaseFactory,
    fingerprintGesturesSupported = fingerprintGesturesSupported,
    pauseKeyMapsUseCase = pauseKeyMapsUseCase,
    settingsRepository = settingsRepository,
    keyEventRelayServiceWrapper = keyEventRelayServiceWrapper,
    inputEventHub = inputEventHub,
    recordTriggerController = recordTriggerController,
    setupAssistantControllerFactory = setupAssistantControllerFactory,
    autoSwitchImeControllerFactory = autoSwitchImeControllerFactory,
) {
    @AssistedFactory
    interface Factory {
        fun create(service: MyAccessibilityService): AccessibilityServiceController
    }
}
