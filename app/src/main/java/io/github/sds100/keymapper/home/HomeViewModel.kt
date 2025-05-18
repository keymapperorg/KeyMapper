package io.github.sds100.keymapper.home

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.base.home.BaseHomeViewModel
import io.github.sds100.keymapper.base.home.ShowHomeScreenAlertsUseCase
import io.github.sds100.keymapper.base.keymaps.ListKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val listKeyMaps: ListKeyMapsUseCase,
    private val pauseKeyMaps: PauseKeyMapsUseCase,
    private val backupRestore: BackupRestoreMappingsUseCase,
    private val showAlertsUseCase: ShowHomeScreenAlertsUseCase,
    private val onboarding: OnboardingUseCase,
    resourceProvider: ResourceProvider,
    private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
    private val sortKeyMaps: SortKeyMapsUseCase,
    private val showInputMethodPickerUseCase: ShowInputMethodPickerUseCase,
    private val buildConfigProvider: BuildConfigProvider,
) : BaseHomeViewModel(
    listKeyMaps,
    pauseKeyMaps,
    backupRestore,
    showAlertsUseCase,
    onboarding,
    resourceProvider,
    setupGuiKeyboard,
    sortKeyMaps,
    showInputMethodPickerUseCase,
    buildConfigProvider,
)
