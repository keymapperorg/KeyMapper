package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.mappings.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.purchasing.PurchasingUseCase
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope

/**
 * Created by sds100 on 24/11/20.
 */

class ConfigTriggerViewModel(
    coroutineScope: CoroutineScope,
    onboarding: OnboardingUseCase,
    config: ConfigKeyMapUseCase,
    recordTrigger: RecordTriggerUseCase,
    createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    displayKeyMap: DisplayKeyMapUseCase,
    resourceProvider: ResourceProvider,
    purchasingUseCase: PurchasingUseCase,
) : BaseConfigTriggerViewModel(
    coroutineScope,
    onboarding,
    config,
    recordTrigger,
    createKeyMapShortcut,
    displayKeyMap,
    resourceProvider,
)
