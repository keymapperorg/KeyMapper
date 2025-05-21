package io.github.sds100.keymapper.keymaps

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.base.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.base.keymaps.BaseConfigKeyMapViewModel
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.trigger.ConfigTriggerViewModel
import javax.inject.Inject

@HiltViewModel
class ConfigKeyMapViewModel @Inject constructor(
    override val configActionsViewModel: ConfigActionsViewModel,
    override val configTriggerViewModel: ConfigTriggerViewModel,
    override val configConstraintsViewModel: ConfigConstraintsViewModel,
    config: ConfigKeyMapUseCase,
    onboarding: OnboardingUseCase,
) : BaseConfigKeyMapViewModel(
    config = config,
    onboarding = onboarding,
)
