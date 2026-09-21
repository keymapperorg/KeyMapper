package io.github.sds100.keymapper.base.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.base.actions.keyevent.FixKeyEventActionDelegate
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.onboarding.SetupAccessibilityServiceDelegate
import io.github.sds100.keymapper.base.onboarding.WhatsNewState
import io.github.sds100.keymapper.base.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.base.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BaseHomeViewModel(
    private val listKeyMaps: ListKeyMapsUseCase,
    private val pauseKeyMaps: PauseKeyMapsUseCase,
    private val backupRestore: BackupRestoreMappingsUseCase,
    private val showAlertsUseCase: ShowHomeScreenAlertsUseCase,
    private val onboarding: OnboardingUseCase,
    resourceProvider: ResourceProvider,
    private val sortKeyMaps: SortKeyMapsUseCase,
    private val showInputMethodPickerUseCase: ShowInputMethodPickerUseCase,
    setupAccessibilityServiceDelegate: SetupAccessibilityServiceDelegate,
    fixKeyEventActionDelegate: FixKeyEventActionDelegate,
    navigationProvider: NavigationProvider,
    dialogProvider: DialogProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    val keyMapListViewModel by lazy {
        KeyMapListViewModel(
            viewModelScope,
            listKeyMaps,
            resourceProvider,
            sortKeyMaps,
            showAlertsUseCase,
            pauseKeyMaps,
            backupRestore,
            showInputMethodPickerUseCase,
            onboarding,
            setupAccessibilityServiceDelegate,
            fixKeyEventActionDelegate,
            navigationProvider,
            dialogProvider,
        )
    }

    private val manuallyShowWhatsNew = MutableStateFlow(false)

    val whatsNewState: StateFlow<WhatsNewState?> = combine(
        onboarding.showWhatsNew,
        manuallyShowWhatsNew,
    ) { showWhatsNew, manualShow ->
        if (showWhatsNew || manualShow) {
            WhatsNewState(
                versionName = onboarding.appVersionName,
                notes = onboarding.getWhatsNewNotes(),
            )
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun onWhatsNewClick() {
        manuallyShowWhatsNew.value = true
    }

    fun onDismissWhatsNew() {
        onboarding.showedWhatsNew()
        manuallyShowWhatsNew.value = false
    }

    fun launchSettings() {
        viewModelScope.launch {
            navigate("settings", NavDestination.Settings)
        }
    }

    fun launchAbout() {
        viewModelScope.launch {
            navigate("about", NavDestination.About)
        }
    }
}

enum class SelectedKeyMapsEnabled {
    ALL,
    NONE,
    MIXED,
}

data class HomeWarningListItem(val id: String, val text: String)
