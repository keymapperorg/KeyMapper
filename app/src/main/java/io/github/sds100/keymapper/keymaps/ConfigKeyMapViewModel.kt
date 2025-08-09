package io.github.sds100.keymapper.keymaps

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.base.actions.CreateActionUseCase
import io.github.sds100.keymapper.base.actions.TestActionUseCase
import io.github.sds100.keymapper.base.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.base.keymaps.BaseConfigKeyMapViewModel
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.base.shortcuts.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.purchasing.PurchasingManager
import io.github.sds100.keymapper.base.trigger.RecordTriggerController
import io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.trigger.ConfigTriggerViewModel
import javax.inject.Inject

@HiltViewModel
class ConfigKeyMapViewModel @Inject constructor(
    display: DisplayKeyMapUseCase,
    config: ConfigKeyMapUseCase,
    onboarding: OnboardingUseCase,
    createActionUseCase: CreateActionUseCase,
    testActionUseCase: TestActionUseCase,
    recordTriggerController: RecordTriggerController,
    createKeyMapShortcutUseCase: CreateKeyMapShortcutUseCase,
    purchasingManager: PurchasingManager,
    setupGuiKeyboardUseCase: SetupGuiKeyboardUseCase,
    fingerprintGesturesSupportedUseCase: FingerprintGesturesSupportedUseCase,
    resourceProvider: ResourceProvider,
    navigationProvider: NavigationProvider,
    dialogProvider: DialogProvider,
) : BaseConfigKeyMapViewModel(
    config = config,
    onboarding = onboarding,
    navigationProvider = navigationProvider,
    dialogProvider = dialogProvider,
) {
    override val configActionsViewModel: ConfigActionsViewModel = ConfigActionsViewModel(
        coroutineScope = viewModelScope,
        displayAction = display,
        createAction = createActionUseCase,
        testAction = testActionUseCase,
        config = config,
        onboarding = onboarding,
        resourceProvider = resourceProvider,
        navigationProvider = navigationProvider,
        dialogProvider = dialogProvider,
    )

    override val configTriggerViewModel: ConfigTriggerViewModel = ConfigTriggerViewModel(
        coroutineScope = viewModelScope,
        onboarding = onboarding,
        config = config,
        recordTrigger = recordTriggerController,
        createKeyMapShortcut = createKeyMapShortcutUseCase,
        displayKeyMap = display,
        purchasingManager = purchasingManager,
        setupGuiKeyboard = setupGuiKeyboardUseCase,
        fingerprintGesturesSupported = fingerprintGesturesSupportedUseCase,
        resourceProvider = resourceProvider,
        navigationProvider = navigationProvider,
        dialogProvider = dialogProvider,
    )

    override val configConstraintsViewModel: ConfigConstraintsViewModel =
        ConfigConstraintsViewModel(
            coroutineScope = viewModelScope,
            config = config,
            displayConstraint = display,
            resourceProvider = resourceProvider,
            navigationProvider = navigationProvider,
            dialogProvider = dialogProvider,
        )
}
