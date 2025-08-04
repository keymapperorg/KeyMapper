package io.github.sds100.keymapper.trigger

import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.purchasing.PurchasingManager
import io.github.sds100.keymapper.base.trigger.BaseConfigTriggerViewModel
import io.github.sds100.keymapper.base.trigger.RecordTriggerController
import io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class ConfigTriggerViewModel @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val onboarding: OnboardingUseCase,
    private val config: ConfigKeyMapUseCase,
    private val recordTrigger: RecordTriggerController,
    private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    private val displayKeyMap: DisplayKeyMapUseCase,
    private val purchasingManager: PurchasingManager,
    private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
    private val fingerprintGesturesSupported: FingerprintGesturesSupportedUseCase,
    resourceProvider: ResourceProvider,
    navigationProvider: NavigationProvider,
    dialogProvider: DialogProvider,
) : BaseConfigTriggerViewModel(
    coroutineScope = coroutineScope,
    onboarding = onboarding,
    config = config,
    recordTrigger = recordTrigger,
    createKeyMapShortcut = createKeyMapShortcut,
    displayKeyMap = displayKeyMap,
    purchasingManager = purchasingManager,
    setupGuiKeyboard = setupGuiKeyboard,
    fingerprintGesturesSupported = fingerprintGesturesSupported,
    resourceProvider = resourceProvider,
    navigationProvider = navigationProvider,
    dialogProvider = dialogProvider,
) {
    override fun onEditFloatingButtonClick() {}

    override fun onEditFloatingLayoutClick() {}
}
