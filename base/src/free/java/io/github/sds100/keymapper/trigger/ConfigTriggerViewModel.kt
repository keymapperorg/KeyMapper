package io.github.sds100.keymapper.trigger

import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.purchasing.PurchasingManager
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
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
