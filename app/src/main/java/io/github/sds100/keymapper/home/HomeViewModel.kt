package io.github.sds100.keymapper.home

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.base.home.BaseHomeViewModel
import io.github.sds100.keymapper.base.home.ListKeyMapsUseCase
import io.github.sds100.keymapper.base.home.ShowHomeScreenAlertsUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.base.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
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
    navigationProvider: NavigationProvider,
    dialogProvider: DialogProvider,
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
    navigationProvider,
    dialogProvider,
)
