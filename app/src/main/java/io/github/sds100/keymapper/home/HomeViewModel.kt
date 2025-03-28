package io.github.sds100.keymapper.home

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.backup.ImportExportState
import io.github.sds100.keymapper.backup.RestoreType
import io.github.sds100.keymapper.floating.FloatingLayoutsState
import io.github.sds100.keymapper.floating.ListFloatingLayoutsUseCase
import io.github.sds100.keymapper.floating.ListFloatingLayoutsViewModel
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.KeyMapListViewModel
import io.github.sds100.keymapper.mappings.keymaps.ListKeyMapsUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.sorting.SortViewModel
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
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
        combine(
            listFloatingLayouts.showFloatingLayouts,
            onboarding.hasViewedAdvancedTriggers,
            transform = ::buildNavBarItems,
        )
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                buildNavBarItems(
                    showFloatingLayouts = false,
                    viewedAdvancedTriggers = false,
                ),
            )

    val keyMapListViewModel by lazy {
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

    var showSortBottomSheet by mutableStateOf(false)

    private val _importExportState = MutableStateFlow<ImportExportState>(ImportExportState.Idle)
    val importExportState: StateFlow<ImportExportState> = _importExportState.asStateFlow()

    private val warnings: Flow<List<HomeWarningListItem>> = combine(
        showAlertsUseCase.isBatteryOptimised,
        showAlertsUseCase.accessibilityServiceState,
        showAlertsUseCase.hideAlerts,
        showAlertsUseCase.isLoggingEnabled,
    ) { isBatteryOptimised, serviceState, isHidden, isLoggingEnabled ->
        if (isHidden) {
            return@combine emptyList()
        }

        buildList {
            when (serviceState) {
                ServiceState.CRASHED ->
                    add(
                        HomeWarningListItem(
                            ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_crashed),
                        ),
                    )

                ServiceState.DISABLED ->
                    add(
                        HomeWarningListItem(
                            ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_disabled),
                        ),
                    )

                ServiceState.ENABLED -> {}
            }

            if (isBatteryOptimised) {
                add(
                    HomeWarningListItem(
                        ID_BATTERY_OPTIMISATION_LIST_ITEM,
                        getString(R.string.home_error_is_battery_optimised),
                    ),
                )
            } // don't show a success message for this

            if (isLoggingEnabled) {
                add(
                    HomeWarningListItem(
                        ID_LOGGING_ENABLED_LIST_ITEM,
                        getString(R.string.home_error_logging_enabled),
                    ),
                )
            }
        }
    }

    val state: StateFlow<HomeState> =
        combine(
            multiSelectProvider.state,
            warnings,
            showAlertsUseCase.areKeyMapsPaused,
            listKeyMaps.keyMapGroup.map { it.keyMaps }.filterIsInstance<State.Data<List<KeyMap>>>(),
            listFloatingLayoutsViewModel.state,
        ) { selectionState, warnings, isPaused, keyMaps, floatingLayoutsState ->

            if (selectionState is SelectionState.Selecting) {

                var selectedKeyMapsEnabled: SelectedKeyMapsEnabled? = null

                for (keyMap in keyMaps.data) {
                    if (keyMap.uid in selectionState.selectedIds) {
                        if (selectedKeyMapsEnabled == null) {
                            if (keyMap.isEnabled) {
                                selectedKeyMapsEnabled = SelectedKeyMapsEnabled.ALL
                            } else {
                                selectedKeyMapsEnabled = SelectedKeyMapsEnabled.NONE
                            }
                        } else {
                            if ((keyMap.isEnabled && selectedKeyMapsEnabled == SelectedKeyMapsEnabled.NONE) ||
                                (!keyMap.isEnabled && selectedKeyMapsEnabled == SelectedKeyMapsEnabled.ALL)
                            ) {
                                selectedKeyMapsEnabled = SelectedKeyMapsEnabled.MIXED
                                break
                            }
                        }
                    }
                }

                HomeState.Selecting(
                    selectionCount = multiSelectProvider.getSelectedIds().size,
                    selectedKeyMapsEnabled = selectedKeyMapsEnabled ?: SelectedKeyMapsEnabled.NONE,
                    isAllSelected = selectionState.selectedIds.size == keyMaps.data.size,
                )
            } else {
                HomeState.Normal(
                    warnings,
                    isPaused,
                    showNewLayoutButton = floatingLayoutsState is FloatingLayoutsState.Purchased,
                )
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
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            if (setupGuiKeyboard.isInstalled.first() && !setupGuiKeyboard.isCompatibleVersion.first()) {
                showUpgradeGuiKeyboardDialog()
            }
        }
    }

    private fun buildNavBarItems(
        showFloatingLayouts: Boolean,
        viewedAdvancedTriggers: Boolean,
    ): List<HomeNavBarItem> {
        val items = mutableListOf<HomeNavBarItem>()
        items.add(
            HomeNavBarItem(
                HomeDestination.KeyMaps,
                getString(R.string.home_nav_bar_key_maps),
                icon = Icons.Outlined.Gamepad,
                badge = null,
            ),
        )

        if (showFloatingLayouts && Build.VERSION.SDK_INT >= Constants.MIN_API_FLOATING_BUTTONS) {
            items.add(
                HomeNavBarItem(
                    HomeDestination.FloatingButtons,
                    getString(R.string.home_nav_bar_floating_buttons),
                    icon = Icons.Outlined.BubbleChart,
                    badge = if (viewedAdvancedTriggers) {
                        null
                    } else {
                        getString(R.string.button_advanced_triggers_badge)
                    },
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
            is Success -> {}

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

    fun onSelectAllClick() {
        state.value.also { state ->
            if (state is HomeState.Selecting) {
                if (state.isAllSelected) {
                    multiSelectProvider.stopSelecting()
                } else {
                    keyMapListViewModel.selectAll()
                }
            }
        }
    }

    fun onEnabledKeyMapsChange(enabled: Boolean) {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting) return
        val selectedIds = selectionState.selectedIds

        if (enabled) {
            listKeyMaps.enableKeyMap(*selectedIds.toTypedArray())
        } else {
            listKeyMaps.disableKeyMap(*selectedIds.toTypedArray())
        }
    }

    fun onDuplicateSelectedKeyMapsClick() {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting) return
        val selectedIds = selectionState.selectedIds

        listKeyMaps.duplicateKeyMap(*selectedIds.toTypedArray())
    }

    fun onDeleteSelectedKeyMapsClick() {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting) return
        val selectedIds = selectionState.selectedIds.toTypedArray()

        listKeyMaps.deleteKeyMap(*selectedIds)
        multiSelectProvider.deselect(*selectedIds)
        multiSelectProvider.stopSelecting()
    }

    fun onExportSelectedKeyMaps() {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting) return

        viewModelScope.launch {
            val selectedIds = selectionState.selectedIds

            listKeyMaps.backupKeyMaps(*selectedIds.toTypedArray()).onSuccess {
                _importExportState.value = ImportExportState.FinishedExport(it)
            }.onFailure {
                _importExportState.value =
                    ImportExportState.Error(it.getFullMessage(this@HomeViewModel))
            }
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
                        popupViewModel = this@HomeViewModel,
                        restartService = showAlertsUseCase::restartAccessibilityService,
                        ignoreCrashed = showAlertsUseCase::acknowledgeCrashed,
                    )

                ID_BATTERY_OPTIMISATION_LIST_ITEM -> showAlertsUseCase.disableBatteryOptimisation()
                ID_LOGGING_ENABLED_LIST_ITEM -> showAlertsUseCase.disableLogging()
            }
        }
    }

    fun onTogglePausedClick() {
        viewModelScope.launch {
            if (pauseMappings.isPaused.first()) {
                pauseMappings.resume()
            } else {
                pauseMappings.pause()
            }
        }
    }

    fun onExportClick() {
        viewModelScope.launch {
            if (_importExportState.value != ImportExportState.Idle) {
                return@launch
            }

            _importExportState.value = ImportExportState.Exporting
            backupRestore.backupEverything().onSuccess {
                _importExportState.value = ImportExportState.FinishedExport(it)
            }.onFailure {
                _importExportState.value =
                    ImportExportState.Error(it.getFullMessage(this@HomeViewModel))
            }
        }
    }

    fun onChooseImportFile(uri: String) {
        viewModelScope.launch {
            backupRestore.getKeyMapCountInBackup(uri).onSuccess {
                _importExportState.value = ImportExportState.ConfirmImport(uri, it)
            }.onFailure {
                _importExportState.value =
                    ImportExportState.Error(it.getFullMessage(this@HomeViewModel))
            }
        }
    }

    fun onConfirmImport(restoreType: RestoreType) {
        val state = _importExportState.value as? ImportExportState.ConfirmImport
        state ?: return

        _importExportState.value = ImportExportState.Importing

        viewModelScope.launch {
            backupRestore.restoreKeyMaps(state.fileUri, restoreType).onSuccess {
                _importExportState.value = ImportExportState.FinishedImport
            }.onFailure {
                _importExportState.value =
                    ImportExportState.Error(it.getFullMessage(this@HomeViewModel))
            }
        }
    }

    fun setImportExportIdle() {
        _importExportState.value = ImportExportState.Idle
    }

    fun onBackClick(): Boolean {
        if (multiSelectProvider.state.value is SelectionState.Selecting) {
            multiSelectProvider.stopSelecting()
            return true
        } else {
            return false
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val listKeyMaps: ListKeyMapsUseCase,
        private val pauseMappings: PauseMappingsUseCase,
        private val backupRestore: BackupRestoreMappingsUseCase,
        private val showAlertsUseCase: ShowHomeScreenAlertsUseCase,
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
            onboarding,
            resourceProvider,
            setupGuiKeyboard,
            sortKeyMaps,
            listFloatingLayouts,
        ) as T
    }
}

sealed class HomeState {
    data class Selecting(
        val selectionCount: Int,
        val selectedKeyMapsEnabled: SelectedKeyMapsEnabled,
        val isAllSelected: Boolean,
    ) : HomeState()

    data class Normal(
        val warnings: List<HomeWarningListItem> = emptyList(),
        val isPaused: Boolean = false,
        val showNewLayoutButton: Boolean = false,
    ) : HomeState()
}

enum class SelectedKeyMapsEnabled {
    ALL,
    NONE,
    MIXED,
}

data class HomeWarningListItem(
    val id: String,
    val text: String,
)

data class HomeNavBarItem(
    val destination: HomeDestination,
    val label: String,
    val icon: ImageVector,
    val badge: String? = null,
)
