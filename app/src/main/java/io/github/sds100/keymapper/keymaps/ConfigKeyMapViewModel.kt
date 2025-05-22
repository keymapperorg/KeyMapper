package io.github.sds100.keymapper.keymaps

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.base.actions.CreateActionUseCase
import io.github.sds100.keymapper.base.actions.TestActionUseCase
import io.github.sds100.keymapper.base.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.base.keymaps.BaseConfigKeyMapViewModel
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.purchasing.PurchasingManager
import io.github.sds100.keymapper.base.trigger.RecordTriggerUseCase
import io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.base.utils.navigation.NavigationViewModel
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.trigger.ConfigTriggerViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConfigKeyMapViewModel @Inject constructor(
    display: DisplayKeyMapUseCase,
    config: ConfigKeyMapUseCase,
    onboarding: OnboardingUseCase,
    createActionUseCase: CreateActionUseCase,
    testActionUseCase: TestActionUseCase,
    recordTriggerUseCase: RecordTriggerUseCase,
    createKeyMapShortcutUseCase: CreateKeyMapShortcutUseCase,
    purchasingManager: PurchasingManager,
    setupGuiKeyboardUseCase: SetupGuiKeyboardUseCase,
    fingerprintGesturesSupportedUseCase: FingerprintGesturesSupportedUseCase,
    resourceProvider: ResourceProvider,
    navigationViewModel: NavigationViewModel
) : BaseConfigKeyMapViewModel(
    config = config,
    onboarding = onboarding,
    navigationViewModel = navigationViewModel
) {
    override val configActionsViewModel: ConfigActionsViewModel = ConfigActionsViewModel(
        coroutineScope = viewModelScope,
        displayAction = display,
        createAction = createActionUseCase,
        testAction = testActionUseCase,
        config = config,
        onboarding = onboarding,
        resourceProvider = resourceProvider,
    )

    override val configTriggerViewModel: ConfigTriggerViewModel = ConfigTriggerViewModel(
        coroutineScope = viewModelScope,
        onboarding = onboarding,
        config = config,
        recordTrigger = recordTriggerUseCase,
        createKeyMapShortcut = createKeyMapShortcutUseCase,
        displayKeyMap = display,
        purchasingManager = purchasingManager,
        setupGuiKeyboard = setupGuiKeyboardUseCase,
        fingerprintGesturesSupported = fingerprintGesturesSupportedUseCase,
        resourceProvider = resourceProvider,
    )

    override val configConstraintsViewModel: ConfigConstraintsViewModel =
        ConfigConstraintsViewModel(
            coroutineScope = viewModelScope,
            config = config,
            displayConstraint = display,
            resourceProvider = resourceProvider,
        )

    init {
        Timber.e("INIT VIEW MODEL")
    }

    override fun onCleared() {
        super.onCleared()

        Timber.e("CLEARED VIEW MODEL")
    }
}
