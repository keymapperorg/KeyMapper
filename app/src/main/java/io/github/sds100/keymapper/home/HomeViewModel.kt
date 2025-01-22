package io.github.sds100.keymapper.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapListViewModel
import io.github.sds100.keymapper.mappings.fingerprintmaps.ListFingerprintMapsUseCase
import io.github.sds100.keymapper.mappings.keymaps.KeyMapListViewModel
import io.github.sds100.keymapper.mappings.keymaps.ListKeyMapsUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.ui.DialogResponse
import io.github.sds100.keymapper.util.ui.ListItem
import io.github.sds100.keymapper.util.ui.MultiSelectProvider
import io.github.sds100.keymapper.util.ui.MultiSelectProviderImpl
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SelectionState
import io.github.sds100.keymapper.util.ui.TextListItem
import io.github.sds100.keymapper.util.ui.ViewModelHelper
import io.github.sds100.keymapper.util.ui.navigate
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 18/01/21.
 */
class HomeViewModel(
    private val listKeyMaps: ListKeyMapsUseCase,
    private val listFingerprintMaps: ListFingerprintMapsUseCase,
    private val pauseMappings: PauseMappingsUseCase,
    private val backupRestore: BackupRestoreMappingsUseCase,
    private val showAlertsUseCase: ShowHomeScreenAlertsUseCase,
    private val showImePicker: ShowInputMethodPickerUseCase,
    private val onboarding: OnboardingUseCase,
    resourceProvider: ResourceProvider,
    private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    private companion object {
        const val ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM = "accessibility_service_disabled"
        const val ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM = "accessibility_service_crashed"
        const val ID_ACCESSIBILITY_SERVICE_ENABLED_LIST_ITEM = "accessibility_service_enabled"
        const val ID_BATTERY_OPTIMISATION_LIST_ITEM = "battery_optimised"
        const val ID_MAPPINGS_PAUSED_LIST_ITEM = "mappings_paused"
        const val ID_LOGGING_ENABLED_LIST_ITEM = "logging_enabled"
    }

    private val multiSelectProvider: MultiSelectProvider<String> = MultiSelectProviderImpl()

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
        )
    }

    val fingerprintMapListViewModel by lazy {
        FingerprintMapListViewModel(
            viewModelScope,
            listFingerprintMaps,
            resourceProvider,
        )
    }

    private val _showQuickStartGuideHint = MutableStateFlow(false)
    val showQuickStartGuideHint = _showQuickStartGuideHint.asStateFlow()

    private val _shareBackup = MutableSharedFlow<String>()
    val shareBackup = _shareBackup.asSharedFlow()

    val selectionCountViewState = multiSelectProvider.state.map {
        when (it) {
            SelectionState.NotSelecting -> SelectionCountViewState(
                isVisible = false,
                text = "",
            )

            is SelectionState.Selecting<*> -> SelectionCountViewState(
                isVisible = true,
                text = getString(R.string.selection_count, it.selectedIds.size),
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            SelectionCountViewState(
                isVisible = false,
                text = "",
            ),
        )

    val tabsState =
        combine(
            multiSelectProvider.state,
            listFingerprintMaps.showFingerprintMaps,
        ) { selectionState, showFingerprintMaps ->

            val tabs = sequence {
                yield(HomeTab.KEY_EVENTS)

                if (showFingerprintMaps) {
                    yield(HomeTab.FINGERPRINT_MAPS)
                }
            }.toList()

            val showTabs = when {
                tabs.size == 1 -> false
                selectionState is SelectionState.Selecting<*> -> false
                else -> true
            }

            HomeTabsState(
                enableViewPagerSwiping = showTabs,
                showTabs = showTabs,
                tabs = tabs,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.Lazily,
                HomeTabsState(
                    enableViewPagerSwiping = false,
                    showTabs = false,
                    emptyList(),
                ),
            )

    val appBarState = multiSelectProvider.state.map {
        when (it) {
            SelectionState.NotSelecting -> HomeAppBarState.NORMAL
            is SelectionState.Selecting<*> -> HomeAppBarState.MULTI_SELECTING
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            HomeAppBarState.NORMAL,
        )

    val errorListState = combine(
        showAlertsUseCase.isBatteryOptimised,
        showAlertsUseCase.accessibilityServiceState,
        showAlertsUseCase.hideAlerts,
        showAlertsUseCase.areMappingsPaused,
        showAlertsUseCase.isLoggingEnabled,
    ) { isBatteryOptimised, serviceState, isHidden, areMappingsPaused, isLoggingEnabled ->
        val listItems = sequence {

            when (serviceState) {
                ServiceState.CRASHED ->
                    yield(
                        TextListItem.Error(
                            ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_crashed),
                        ),
                    )

                ServiceState.DISABLED ->
                    yield(
                        TextListItem.Error(
                            ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_disabled),
                        ),
                    )

                ServiceState.ENABLED ->
                    yield(
                        TextListItem.Success(
                            ID_ACCESSIBILITY_SERVICE_ENABLED_LIST_ITEM,
                            getString(R.string.home_success_accessibility_service_is_enabled),
                        ),
                    )
            }

            if (isBatteryOptimised) {
                yield(
                    TextListItem.Error(
                        ID_BATTERY_OPTIMISATION_LIST_ITEM,
                        getString(R.string.home_error_is_battery_optimised),
                    ),
                )
            } // don't show a success message for this

            if (areMappingsPaused) {
                yield(
                    TextListItem.Error(
                        ID_MAPPINGS_PAUSED_LIST_ITEM,
                        getString(R.string.home_error_key_maps_paused),
                        customButtonText = getString(R.string.home_error_key_maps_paused_button),
                    ),
                )
            }

            if (isLoggingEnabled) {
                yield(
                    TextListItem.Error(
                        ID_LOGGING_ENABLED_LIST_ITEM,
                        getString(R.string.home_error_logging_enabled),
                        customButtonText = getString(R.string.home_error_logging_enabled_button),
                    ),
                )
            }
        }.toList()
        HomeErrorListState(listItems, !isHidden)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, HomeErrorListState(emptyList(), false))

    private val _showMenu = MutableSharedFlow<Unit>()
    val showMenu = _showMenu.asSharedFlow()

    private val _closeKeyMapper = MutableSharedFlow<Unit>()
    val closeKeyMapper = _closeKeyMapper.asSharedFlow()

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

    fun onAppBarNavigationButtonClick() {
        viewModelScope.launch {
            if (multiSelectProvider.state.value is SelectionState.Selecting<*>) {
                multiSelectProvider.stopSelecting()
            } else {
                _showMenu.emit(Unit)
            }
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            if (multiSelectProvider.state.value is SelectionState.Selecting<*>) {
                multiSelectProvider.stopSelecting()
            } else {
                _closeKeyMapper.emit(Unit)
            }
        }
    }

    fun onFabPressed() {
        viewModelScope.launch {
            val selectionState = multiSelectProvider.state.value
            if (selectionState is SelectionState.Selecting<*>) {
                val selectedIds = selectionState.selectedIds as Set<String>

                listKeyMaps.deleteKeyMap(*selectedIds.toTypedArray())

                multiSelectProvider.stopSelecting()
            } else {
                navigate("create_new_keymap", NavDestination.ConfigKeyMap(keyMapUid = null))
            }
        }
    }

    fun onSelectAllClick() {
        keymapListViewModel.selectAll()
    }

    fun onEnableSelectedKeymapsClick() {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting<*>) return
        val selectedIds = selectionState.selectedIds as Set<String>

        listKeyMaps.enableKeyMap(*selectedIds.toTypedArray())
    }

    fun onDisableSelectedKeymapsClick() {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting<*>) return
        val selectedIds = selectionState.selectedIds as Set<String>

        listKeyMaps.disableKeyMap(*selectedIds.toTypedArray())
    }

    fun onDuplicateSelectedKeymapsClick() {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting<*>) return
        val selectedIds = selectionState.selectedIds as Set<String>

        listKeyMaps.duplicateKeyMap(*selectedIds.toTypedArray())
    }

    fun backupFingerprintMaps(uri: String) {
        viewModelScope.launch {
            val result = listFingerprintMaps.backupFingerprintMaps(uri)

            onBackupResult(result)
        }
    }

    fun backupSelectedKeyMaps(uri: String) {
        viewModelScope.launch {
            val selectionState = multiSelectProvider.state.first()

            if (selectionState !is SelectionState.Selecting<*>) return@launch

            val selectedIds = selectionState.selectedIds as Set<String>

            launch {
                val result = listKeyMaps.backupKeyMaps(*selectedIds.toTypedArray(), uri = uri)

                onBackupResult(result)
            }

            multiSelectProvider.stopSelecting()
        }
    }

    fun onFixErrorListItemClick(id: String) {
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
                ID_MAPPINGS_PAUSED_LIST_ITEM -> showAlertsUseCase.resumeMappings()
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
        private val listFingerprintMaps: ListFingerprintMapsUseCase,
        private val pauseMappings: PauseMappingsUseCase,
        private val backupRestore: BackupRestoreMappingsUseCase,
        private val showAlertsUseCase: ShowHomeScreenAlertsUseCase,
        private val showImePicker: ShowInputMethodPickerUseCase,
        private val onboarding: OnboardingUseCase,
        private val resourceProvider: ResourceProvider,
        private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(
            listKeyMaps,
            listFingerprintMaps,
            pauseMappings,
            backupRestore,
            showAlertsUseCase,
            showImePicker,
            onboarding,
            resourceProvider,
            setupGuiKeyboard,
        ) as T
    }
}

data class SelectionCountViewState(
    val isVisible: Boolean,
    val text: String,
)

enum class HomeAppBarState {
    NORMAL,
    MULTI_SELECTING,
}

data class HomeTabsState(
    val enableViewPagerSwiping: Boolean = true,
    val showTabs: Boolean = false,
    val tabs: List<HomeTab>,
)

data class HomeErrorListState(
    val listItems: List<ListItem>,
    val isVisible: Boolean,
)
