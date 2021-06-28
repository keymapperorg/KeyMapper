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
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl() {

    private companion object {
        const val ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM = "accessibility_service_disabled"
        const val ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM = "accessibility_service_crashed"
        const val ID_ACCESSIBILITY_SERVICE_ENABLED_LIST_ITEM = "accessibility_service_enabled"
        const val ID_BATTERY_OPTIMISATION_LIST_ITEM = "battery_optimised"
        const val ID_MAPPINGS_PAUSED_LIST_ITEM = "mappings_paused"
        const val ID_LOGGING_ENABLED_LIST_ITEM = "logging_enabled"
    }

    private val multiSelectProvider: MultiSelectProvider = MultiSelectProviderImpl()

    val menuViewModel = HomeMenuViewModel(
        viewModelScope,
        showAlertsUseCase,
        pauseMappings,
        backupRestore,
        showImePicker,
        resourceProvider
    )

    val keymapListViewModel = KeyMapListViewModel(
        viewModelScope,
        listKeyMaps,
        resourceProvider,
        multiSelectProvider
    )

    val fingerprintMapListViewModel = FingerprintMapListViewModel(
        viewModelScope,
        listFingerprintMaps,
        resourceProvider
    )

    private val _openUrl = MutableSharedFlow<String>()
    val openUrl = _openUrl.asSharedFlow()

    private val _openSettings = MutableSharedFlow<Unit>()
    val openSettings = _openSettings.asSharedFlow()

    private val _showQuickStartGuideHint = MutableStateFlow(false)
    val showQuickStartGuideHint = _showQuickStartGuideHint.asStateFlow()

    val selectionCountViewState = multiSelectProvider.state.map {
        when (it) {
            SelectionState.NotSelecting -> SelectionCountViewState(
                isVisible = false,
                text = ""
            )
            is SelectionState.Selecting -> SelectionCountViewState(
                isVisible = true,
                text = getString(R.string.selection_count, it.selectedIds.size)
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        SelectionCountViewState(
            isVisible = false,
            text = ""
        )
    )

    val tabsState =
        combine(
            multiSelectProvider.state,
            listFingerprintMaps.showFingerprintMaps
        ) { selectionState, showFingerprintMaps ->

            val tabs = sequence {
                yield(HomeTab.KEY_EVENTS)

                if (showFingerprintMaps) {
                    yield(HomeTab.FINGERPRINT_MAPS)
                }
            }.toSet()

            val showTabs = when {
                tabs.size == 1 -> false
                selectionState is SelectionState.Selecting -> false
                else -> true
            }

            HomeTabsState(
                enableViewPagerSwiping = showTabs,
                showTabs = showTabs,
                tabs = tabs
            )

        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            HomeTabsState(
                enableViewPagerSwiping = false,
                showTabs = false,
                emptySet()
            )
        )

    val appBarState = multiSelectProvider.state.map {
        when (it) {
            SelectionState.NotSelecting -> HomeAppBarState.NORMAL
            is SelectionState.Selecting -> HomeAppBarState.MULTI_SELECTING
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        HomeAppBarState.NORMAL
    )

    val errorListState = combine(
        showAlertsUseCase.isBatteryOptimised,
        showAlertsUseCase.serviceState,
        showAlertsUseCase.hideAlerts,
        showAlertsUseCase.areMappingsPaused,
        showAlertsUseCase.isLoggingEnabled
    ) { isBatteryOptimised, serviceState, isHidden, areMappingsPaused, isLoggingEnabled ->
        val listItems = sequence {

            when (serviceState) {
                ServiceState.CRASHED ->
                    yield(
                        TextListItem.Error(
                            ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_crashed)
                        )
                    )
                ServiceState.DISABLED ->
                    yield(
                        TextListItem.Error(
                            ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_disabled)
                        )
                    )
                ServiceState.ENABLED ->
                    yield(
                        TextListItem.Success(
                            ID_ACCESSIBILITY_SERVICE_ENABLED_LIST_ITEM,
                            getString(R.string.home_success_accessibility_service_is_enabled)
                        )
                    )
            }

            if (isBatteryOptimised) {
                yield(
                    TextListItem.Error(
                        ID_BATTERY_OPTIMISATION_LIST_ITEM,
                        getString(R.string.home_error_is_battery_optimised)
                    )
                )
            } // don't show a success message for this

            if (areMappingsPaused) {
                yield(
                    TextListItem.Error(
                        ID_MAPPINGS_PAUSED_LIST_ITEM,
                        getString(R.string.home_error_key_maps_paused),
                        customButtonText = getString(R.string.home_error_key_maps_paused_button)
                    )
                )
            }

            if (isLoggingEnabled) {
                yield(
                    TextListItem.Error(
                        ID_LOGGING_ENABLED_LIST_ITEM,
                        getString(R.string.home_error_logging_enabled),
                        customButtonText = getString(R.string.home_error_logging_enabled_button)
                    )
                )
            }
        }.toList()
        HomeErrorListState(listItems, !isHidden)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeErrorListState(emptyList(), false))

    private val _showMenu = MutableSharedFlow<Unit>()
    val showMenu = _showMenu.asSharedFlow()

    private val _navigateToCreateKeymapScreen = MutableSharedFlow<Unit>()
    val navigateToCreateKeymapScreen = _navigateToCreateKeymapScreen.asSharedFlow()

    private val _closeKeyMapper = MutableSharedFlow<Unit>()
    val closeKeyMapper = _closeKeyMapper.asSharedFlow()

    init {
        viewModelScope.launch {
            backupRestore.onBackupResult.collectLatest { result ->
                when (result) {
                    is Success -> {
                        showPopup(
                            "successful_backup_result",
                            PopupUi.SnackBar(getString(R.string.toast_backup_successful))
                        )
                    }

                    is Error -> showPopup(
                        "backup_error",
                        PopupUi.Ok(
                            title = getString(R.string.toast_backup_failed),
                            message = result.getFullMessage(this@HomeViewModel)
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            backupRestore.onRestoreResult.collectLatest { result ->
                when (result) {
                    is Success -> {
                        showPopup(
                            "successful_restore_result",
                            PopupUi.SnackBar(getString(R.string.toast_restore_successful))
                        )
                    }

                    is Error -> showPopup(
                        "restore_error",
                        PopupUi.Ok(
                            title = getString(R.string.toast_restore_failed),
                            message = result.getFullMessage(this@HomeViewModel)
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            backupRestore.onAutomaticBackupResult.collectLatest { result ->
                when (result) {
                    is Success -> {
                        showPopup(
                            "successful_automatic_backup_result",
                            PopupUi.SnackBar(getString(R.string.toast_automatic_backup_successful))
                        )
                    }

                    is Error -> {
                        val response = showPopup(
                            "automatic_backup_error",
                            PopupUi.Dialog(
                                title = getString(R.string.toast_automatic_backup_failed),
                                message = result.getFullMessage(this@HomeViewModel),
                                positiveButtonText = getString(R.string.pos_ok),
                                neutralButtonText = getString(R.string.neutral_go_to_settings)
                            )
                        ) ?: return@collectLatest

                        if (response == DialogResponse.NEUTRAL) {
                            _openSettings.emit(Unit)
                        }
                    }
                }
            }
        }

        combine(
            onboarding.showWhatsNew,
            onboarding.showQuickStartGuideHint
        ) { showWhatsNew, showQuickStartGuideHint ->

            if (showWhatsNew) {
                val dialog = PopupUi.Dialog(
                    title = getString(R.string.whats_new),
                    message = onboarding.getWhatsNewText(),
                    positiveButtonText = getString(R.string.pos_ok),
                    neutralButtonText = getString(R.string.neutral_changelog)
                )

                //don't return if they dismiss the dialog because this is common behaviour.
                val response = showPopup("whats-new", dialog)

                if (response == DialogResponse.NEUTRAL) {
                    _openUrl.emit(getString(R.string.url_changelog))
                }

                onboarding.showedWhatsNew()
            }

            _showQuickStartGuideHint.value = showQuickStartGuideHint

        }.launchIn(viewModelScope)
    }

    fun approvedQuickStartGuideTapTarget() {
        onboarding.shownQuickStartGuideHint()
    }

    fun onAppBarNavigationButtonClick() {
        viewModelScope.launch {
            if (multiSelectProvider.state.value is SelectionState.Selecting) {
                multiSelectProvider.stopSelecting()
            } else {
                _showMenu.emit(Unit)
            }
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            if (multiSelectProvider.state.value is SelectionState.Selecting) {
                multiSelectProvider.stopSelecting()
            } else {
                _closeKeyMapper.emit(Unit)
            }
        }
    }

    fun onFabPressed() {
        viewModelScope.launch {
            if (multiSelectProvider.state.value is SelectionState.Selecting) {
                multiSelectProvider.state.value.apply {
                    if (this is SelectionState.Selecting) {
                        listKeyMaps.deleteKeyMap(*selectedIds.toTypedArray())
                    }
                }
                multiSelectProvider.stopSelecting()
            } else {
                _navigateToCreateKeymapScreen.emit(Unit)
            }
        }
    }

    fun onSelectAllClick() {
        keymapListViewModel.selectAll()
    }

    fun onEnableSelectedKeymapsClick() {
        multiSelectProvider.state.value.apply {
            if (this !is SelectionState.Selecting) return
            listKeyMaps.enableKeyMap(*selectedIds.toTypedArray())
        }
    }

    fun onDisableSelectedKeymapsClick() {
        multiSelectProvider.state.value.apply {
            if (this !is SelectionState.Selecting) return
            listKeyMaps.disableKeyMap(*selectedIds.toTypedArray())
        }
    }

    fun onDuplicateSelectedKeymapsClick() {
        multiSelectProvider.state.value.apply {
            if (this !is SelectionState.Selecting) return
            listKeyMaps.duplicateKeyMap(*selectedIds.toTypedArray())
        }
    }

    fun backupFingerprintMaps(uri: String) {
        listFingerprintMaps.backupFingerprintMaps(uri)
    }

    fun backupSelectedKeyMaps(uri: String) {
        viewModelScope.launch {
            val selectionState = multiSelectProvider.state.first()

            if (selectionState !is SelectionState.Selecting) return@launch

            val selectedIds = selectionState.selectedIds

            listKeyMaps.backupKeyMaps(*selectedIds.toTypedArray(), uri = uri)

            multiSelectProvider.stopSelecting()
        }
    }

    fun onFixErrorListItemClick(id: String) {
        viewModelScope.launch {
            when (id) {
                ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM -> showAlertsUseCase.enableAccessibilityService()
                ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM -> showAlertsUseCase.restartAccessibilityService()
                ID_BATTERY_OPTIMISATION_LIST_ITEM -> showAlertsUseCase.disableBatteryOptimisation()
                ID_MAPPINGS_PAUSED_LIST_ITEM -> showAlertsUseCase.resumeMappings()
                ID_LOGGING_ENABLED_LIST_ITEM -> showAlertsUseCase.disableLogging()
            }
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
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return HomeViewModel(
                listKeyMaps,
                listFingerprintMaps,
                pauseMappings,
                backupRestore,
                showAlertsUseCase,
                showImePicker,
                onboarding,
                resourceProvider
            ) as T
        }
    }
}

data class SelectionCountViewState(
    val isVisible: Boolean,
    val text: String
)

enum class HomeAppBarState {
    NORMAL, MULTI_SELECTING
}

data class HomeTabsState(
    val enableViewPagerSwiping: Boolean = true,
    val showTabs: Boolean = false,
    val tabs: Set<HomeTab>
)

data class HomeErrorListState(
    val listItems: List<ListItem>,
    val isVisible: Boolean
)