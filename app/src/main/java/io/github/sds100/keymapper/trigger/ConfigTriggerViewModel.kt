package io.github.sds100.keymapper.trigger

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingTipDelegate
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.onboarding.SetupAccessibilityServiceDelegate
import io.github.sds100.keymapper.base.shortcuts.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.base.trigger.BaseConfigTriggerViewModel
import io.github.sds100.keymapper.base.trigger.ConfigTriggerUseCase
import io.github.sds100.keymapper.base.trigger.RecordTriggerController
import io.github.sds100.keymapper.base.trigger.TriggerSetupDelegate
import io.github.sds100.keymapper.base.trigger.TriggerSetupShortcut
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigTriggerViewModel @Inject constructor(
    private val onboarding: OnboardingUseCase,
    private val config: ConfigTriggerUseCase,
    private val recordTrigger: RecordTriggerController,
    private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    private val displayKeyMap: DisplayKeyMapUseCase,
    private val fingerprintGesturesSupported: FingerprintGesturesSupportedUseCase,
    setupAccessibilityServiceDelegate: SetupAccessibilityServiceDelegate,
    onboardingTipDelegate: OnboardingTipDelegate,
    triggerSetupDelegate: TriggerSetupDelegate,
    resourceProvider: ResourceProvider,
    navigationProvider: NavigationProvider,
    dialogProvider: DialogProvider,
) : BaseConfigTriggerViewModel(
    onboarding = onboarding,
    config = config,
    recordTrigger = recordTrigger,
    createKeyMapShortcut = createKeyMapShortcut,
    displayKeyMap = displayKeyMap,
    fingerprintGesturesSupported = fingerprintGesturesSupported,
    setupAccessibilityServiceDelegate = setupAccessibilityServiceDelegate,
    onboardingTipDelegate = onboardingTipDelegate,
    triggerSetupDelegate = triggerSetupDelegate,
    resourceProvider = resourceProvider,
    navigationProvider = navigationProvider,
    dialogProvider = dialogProvider,
) {
    override fun onEditFloatingButtonClick() {}

    override fun onEditFloatingLayoutClick() {}

    override fun showTriggerSetup(shortcut: TriggerSetupShortcut, forceProMode: Boolean) {
        when (shortcut) {
            TriggerSetupShortcut.ASSISTANT -> viewModelScope.launch {
                navigateToAdvancedTriggers("purchase_assistant_trigger")
            }

            else -> super.showTriggerSetup(shortcut, forceProMode)
        }
    }
}
