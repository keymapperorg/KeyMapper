package io.github.sds100.keymapper.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.floating.ListFloatingLayoutsUseCase
import io.github.sds100.keymapper.floating.ListFloatingLayoutsViewModel
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.mappings.keymaps.KeyMapListViewModel
import io.github.sds100.keymapper.mappings.keymaps.ListKeyMapsUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.sorting.SortViewModel
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.ui.DialogResponse
import io.github.sds100.keymapper.util.ui.MultiSelectProvider
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SelectionState
import io.github.sds100.keymapper.util.ui.ViewModelHelper
import io.github.sds100.keymapper.util.ui.navigate
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 18/01/21.
 */
class HomeViewModel(
    private val listKeyMaps: ListKeyMapsUseCase,
    private val pauseMappings: PauseMappingsUseCase,
    private val backupRestore: BackupRestoreMappingsUseCase,
    private val showAlertsUseCase: ShowHomeScreenAlertsUseCase,
    private val showImePicker: ShowInputMethodPickerUseCase,
    private val onboarding: OnboardingUseCase,
    resourceProvider: ResourceProvider,
    private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
    private val sortKeyMaps: SortKeyMapsUseCase,
    private val listFloatingLayouts: ListFloatingLayoutsUseCase,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    private companion object {
        const val ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM = "accessibility_service_disabled"
        const val ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM = "accessibility_service_crashed"
        const val ID_BATTERY_OPTIMISATION_LIST_ITEM = "battery_optimised"
        const val ID_LOGGING_ENABLED_LIST_ITEM = "logging_enabled"
    }

    private val multiSelectProvider: MultiSelectProvider = MultiSelectProvider()
    val navBarItems: StateFlow<List<HomeNavBarItem>> =
        listFloatingLayouts.showFloatingLayouts
            .map(::buildNavBarItems)
            .stateIn(viewModelScope, SharingStarted.Eagerly, buildNavBarItems(false))

    val menuViewModel by lazy {
        HomeMenuViewModel(
            viewModelScope,
            showAlertsUseCase,
            pauseMappings,
            showImePicker,
            resourceProvider,
        )
    }

    val keymapListViewModel by lazy {
        KeyMapListViewModel(
            viewModelScope,
            listKeyMaps,
            resourceProvider,
            multiSelectProvider,
            setupGuiKeyboard,
            sortKeyMaps,
        )
    }

    val listFloatingLayoutsViewModel by lazy {
        ListFloatingLayoutsViewModel(
            viewModelScope,
            listFloatingLayouts,
            resourceProvider,
        )
    }

    val sortViewModel by lazy {
        SortViewModel(viewModelScope, sortKeyMaps)
    }

    private val _showQuickStartGuideHint = MutableStateFlow(false)
    val showQuickStartGuideHint = _showQuickStartGuideHint.asStateFlow()

    private val _shareBackup = MutableSharedFlow<String>()
    val shareBackup = _shareBackup.asSharedFlow()

    private val warnings: Flow<List<HomeWarningListItem>> = combine(
        showAlertsUseCase.isBatteryOptimised,
        showAlertsUseCase.accessibilityServiceState,
        showAlertsUseCase.hideAlerts,
        showAlertsUseCase.isLoggingEnabled,
    ) { isBatteryOptimised, serviceState, isHidden, isLoggingEnabled ->
        sequence {
            when (serviceState) {
                ServiceState.CRASHED ->
                    yield(
                        HomeWarningListItem(
                            ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_crashed),
                        ),
                    )

                ServiceState.DISABLED ->
                    yield(
                        HomeWarningListItem(
                            ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_disabled),
                        ),
                    )

                ServiceState.ENABLED -> {}
            }

            if (isBatteryOptimised) {
                yield(
                    HomeWarningListItem(
                        ID_BATTERY_OPTIMISATION_LIST_ITEM,
                        getString(R.string.home_error_is_battery_optimised),
                    ),
                )
            } // don't show a success message for this

            if (isLoggingEnabled) {
                yield(
                    HomeWarningListItem(
                        ID_LOGGING_ENABLED_LIST_ITEM,
                        getString(R.string.home_error_logging_enabled),
                    ),
                )
            }
        }.toList()
    }

    val state: StateFlow<HomeState> =
        combine(
            multiSelectProvider.state,
            warnings,
            showAlertsUseCase.areKeyMapsPaused,
            listKeyMaps.areAllEnabled,
        ) { selectionState, warnings, isPaused, areAllEnabled ->
            if (selectionState !is SelectionState.Selecting) {
                HomeState.Selecting(
                    multiSelectProvider.getSelectedIds().size,
                    areAllEnabled,
                )
            } else {
                HomeState.Normal(warnings, isPaused)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState.Normal())

    init {
        viewModelScope.launch {
            backupRestore.onAutomaticBackupResult.collectLatest { result ->
                onAutomaticBackupResult(result)
            }
        }

        combine(
            onboarding.showWhatsNew,
            onboarding.showQuickStartGuideHint,
        ) { showWhatsNew, showQuickStartGuideHint ->

            if (showWhatsNew) {
                showWhatsNewDialog()
            }

            _showQuickStartGuideHint.value = showQuickStartGuideHint
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            if (setupGuiKeyboard.isInstalled.first() && !setupGuiKeyboard.isCompatibleVersion.first()) {
                showUpgradeGuiKeyboardDialog()
            }
        }
    }

    private fun buildNavBarItems(showFloatingLayouts: Boolean): List<HomeNavBarItem> {
        val items = mutableListOf<HomeNavBarItem>()
        items.add(
            HomeNavBarItem(
                HomeDestination.KeyMaps,
                getString(R.string.home_nav_bar_key_maps),
                icon = Icons.Outlined.Gamepad,
            ),
        )

        if (showFloatingLayouts) {
            items.add(
                HomeNavBarItem(
                    HomeDestination.FloatingButtons,
                    getString(R.string.home_nav_bar_floating_buttons),
                    icon = Icons.Outlined.BubbleChart,
                ),
            )
        }

        return items
    }

    private suspend fun showWhatsNewDialog() {
        val dialog = PopupUi.Dialog(
            title = getString(R.string.whats_new),
            message = onboarding.getWhatsNewText(),
            positiveButtonText = getString(R.string.pos_ok),
            neutralButtonText = getString(R.string.neutral_changelog),
        )

        // don't return if they dismiss the dialog because this is common behaviour.
        val response = showPopup("whats-new", dialog)

        if (response == DialogResponse.NEUTRAL) {
            showPopup("url_changelog", PopupUi.OpenUrl(getString(R.string.url_changelog)))
        }

        onboarding.showedWhatsNew()
    }

    private suspend fun onAutomaticBackupResult(result: Result<*>) {
        when (result) {
            is Success -> {
                showPopup(
                    "successful_automatic_backup_result",
                    PopupUi.SnackBar(getString(R.string.toast_automatic_backup_successful)),
                )
            }

            is Error -> {
                val response = showPopup(
                    "automatic_backup_error",
                    PopupUi.Dialog(
                        title = getString(R.string.toast_automatic_backup_failed),
                        message = result.getFullMessage(this),
                        positiveButtonText = getString(R.string.pos_ok),
                        neutralButtonText = getString(R.string.neutral_go_to_settings),
                    ),
                ) ?: return

                if (response == DialogResponse.NEUTRAL) {
                    navigate("settings", NavDestination.Settings)
                }
            }
        }
    }

    private suspend fun showUpgradeGuiKeyboardDialog() {
        val dialog = PopupUi.Dialog(
            title = getString(R.string.dialog_upgrade_gui_keyboard_title),
            message = getString(R.string.dialog_upgrade_gui_keyboard_message),
            positiveButtonText = getString(R.string.dialog_upgrade_gui_keyboard_positive),
            negativeButtonText = getString(R.string.dialog_upgrade_gui_keyboard_neutral),
        )

        val response = showPopup("upgrade_gui_keyboard", dialog)

        if (response == DialogResponse.POSITIVE) {
            showPopup(
                "gui_keyboard_play_store",
                PopupUi.OpenUrl(getString(R.string.url_play_store_keymapper_gui_keyboard)),
            )
        }
    }

    fun approvedQuickStartGuideTapTarget() {
        onboarding.shownQuickStartGuideHint()
    }

    fun onSelectAllClick() {
        keymapListViewModel.selectAll()
    }

    fun onSetKeyMapsEnabledClick(enabled: Boolean) {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting) return
        val selectedIds = selectionState.selectedIds

        if (enabled) {
            listKeyMaps.enableKeyMap(*selectedIds.toTypedArray())
        } else {
            listKeyMaps.disableKeyMap(*selectedIds.toTypedArray())
        }
    }

    fun onDuplicateSelectedKeymapsClick() {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting) return
        val selectedIds = selectionState.selectedIds

        listKeyMaps.duplicateKeyMap(*selectedIds.toTypedArray())
    }

    fun backupSelectedKeyMaps(uri: String) {
        viewModelScope.launch {
            val selectionState = multiSelectProvider.state.first()

            if (selectionState !is SelectionState.Selecting) return@launch

            val selectedIds = selectionState.selectedIds

            launch {
                val result = listKeyMaps.backupKeyMaps(*selectedIds.toTypedArray(), uri = uri)

                onBackupResult(result)
            }

            multiSelectProvider.stopSelecting()
        }
    }

    fun onFixWarningClick(id: String) {
        viewModelScope.launch {
            when (id) {
                ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM -> {
                    val explanationResponse =
                        ViewModelHelper.showAccessibilityServiceExplanationDialog(
                            resourceProvider = this@HomeViewModel,
                            popupViewModel = this@HomeViewModel,
                        )

                    if (explanationResponse != DialogResponse.POSITIVE) {
                        return@launch
                    }

                    if (!showAlertsUseCase.startAccessibilityService()) {
                        ViewModelHelper.handleCantFindAccessibilitySettings(
                            resourceProvider = this@HomeViewModel,
                            popupViewModel = this@HomeViewModel,
                        )
                    }
                }

                ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM ->
                    ViewModelHelper.handleKeyMapperCrashedDialog(
                        resourceProvider = this@HomeViewModel,
                        navigationViewModel = this@HomeViewModel,
                        popupViewModel = this@HomeViewModel,
                        restartService = showAlertsUseCase::restartAccessibilityService,
                    )

                ID_BATTERY_OPTIMISATION_LIST_ITEM -> showAlertsUseCase.disableBatteryOptimisation()
                ID_LOGGING_ENABLED_LIST_ITEM -> showAlertsUseCase.disableLogging()
            }
        }
    }

    fun onChoseRestoreFile(uri: String) {
        viewModelScope.launch {
            when (val result = backupRestore.restoreMappings(uri)) {
                is Success -> {
                    showPopup(
                        "successful_restore_result",
                        PopupUi.SnackBar(getString(R.string.toast_restore_successful)),
                    )
                }

                is Error -> showPopup(
                    "restore_error",
                    PopupUi.Ok(
                        title = getString(R.string.toast_restore_failed),
                        message = result.getFullMessage(this@HomeViewModel),
                    ),
                )
            }
        }
    }

    fun onChoseBackupFile(uri: String) {
        viewModelScope.launch {
            val result = backupRestore.backupAllMappings(uri)

            onBackupResult(result)
        }
    }

    private suspend fun onBackupResult(result: Result<String>) {
        when (result) {
            is Success -> {
                val response = showPopup(
                    "successful_backup_result",
                    PopupUi.SnackBar(
                        message = getString(R.string.toast_backup_successful),
                        actionText = getString(R.string.share_backup),
                    ),
                )

                if (response != null) {
                    val uri = result.value

                    _shareBackup.emit(uri)
                }
            }

            is Error -> showPopup(
                "backup_error",
                PopupUi.Ok(
                    title = getString(R.string.toast_backup_failed),
                    message = result.getFullMessage(this@HomeViewModel),
                ),
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val listKeyMaps: ListKeyMapsUseCase,
        private val pauseMappings: PauseMappingsUseCase,
        private val backupRestore: BackupRestoreMappingsUseCase,
        private val showAlertsUseCase: ShowHomeScreenAlertsUseCase,
        private val showImePicker: ShowInputMethodPickerUseCase,
        private val onboarding: OnboardingUseCase,
        private val resourceProvider: ResourceProvider,
        private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
        private val sortKeyMaps: SortKeyMapsUseCase,
        private val listFloatingLayouts: ListFloatingLayoutsUseCase,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(
            listKeyMaps,
            pauseMappings,
            backupRestore,
            showAlertsUseCase,
            showImePicker,
            onboarding,
            resourceProvider,
            setupGuiKeyboard,
            sortKeyMaps,
            listFloatingLayouts,
        ) as T
    }
}

sealed class HomeState {
    data class Selecting(val selectionCount: Int, val allKeyMapsEnabled: Boolean) : HomeState()
    data class Normal(
        val warnings: List<HomeWarningListItem> = emptyList(),
        val isPaused: Boolean = false,
    ) : HomeState()
}

data class HomeWarningListItem(
    val id: String,
    val text: String,
)

data class HomeNavBarItem(
    val destination: HomeDestination,
    val label: String,
    val icon: ImageVector,
)
