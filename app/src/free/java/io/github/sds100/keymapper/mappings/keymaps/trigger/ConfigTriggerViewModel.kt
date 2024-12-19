package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.mappings.keymaps.DisplayKeyMapUseCase
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
    private val purchasingManager: PurchasingManager,
) : BaseConfigTriggerViewModel(
    coroutineScope,
    onboarding,
    config,
    recordTrigger,
    createKeyMapShortcut,
    displayKeyMap,
    resourceProvider,
)
