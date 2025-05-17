package io.github.sds100.keymapper.trigger

import io.github.sds100.keymapper.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.keymaps.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.purchasing.PurchasingManager
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope

class ConfigTriggerViewModel(
    coroutineScope: CoroutineScope,
    onboarding: OnboardingUseCase,
    config: ConfigKeyMapUseCase,
    recordTrigger: RecordTriggerUseCase,
    createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    displayKeyMap: DisplayKeyMapUseCase,
    resourceProvider: ResourceProvider,
    purchasingManager: PurchasingManager,
    setupGuiKeyboardUseCase: SetupGuiKeyboardUseCase,
    fingerprintGesturesSupported: FingerprintGesturesSupportedUseCase,
) : BaseConfigTriggerViewModel(
    coroutineScope,
    onboarding,
    config,
    recordTrigger,
    createKeyMapShortcut,
    displayKeyMap,
    purchasingManager,
    setupGuiKeyboardUseCase,
    fingerprintGesturesSupported,
    resourceProvider,
) {
    fun onEditFloatingButtonClick() {}
    fun onEditFloatingLayoutClick() {}
}
