package io.github.sds100.keymapper.home

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 17/11/20.
 */
class HomeMenuViewModel(
    private val coroutineScope: CoroutineScope,
    private val alertsUseCase: ShowHomeScreenAlertsUseCase,
    private val pauseMappings: PauseMappingsUseCase,
    private val showImePicker: ShowInputMethodPickerUseCase,
    resourceProvider: ResourceProvider
) : ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    val toggleMappingsButtonState: StateFlow<ToggleMappingsButtonState?> =
        combine(
            pauseMappings.isPaused,
            alertsUseCase.accessibilityServiceState
        ) { isPaused, serviceState ->
            when (serviceState) {
                ServiceState.ENABLED ->
                    if (isPaused) {
                        ToggleMappingsButtonState.PAUSED
                    } else {
                        ToggleMappingsButtonState.RESUMED
                    }
                ServiceState.CRASHED -> ToggleMappingsButtonState.SERVICE_CRASHED

                ServiceState.DISABLED -> ToggleMappingsButtonState.SERVICE_DISABLED
            }

        }.stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private val _chooseBackupFile = MutableSharedFlow<Unit>()
    val chooseBackupFile = _chooseBackupFile.asSharedFlow()

    private val _chooseRestoreFile = MutableSharedFlow<Unit>()
    val chooseRestoreFile = _chooseRestoreFile.asSharedFlow()

    private val _dismiss = MutableSharedFlow<Unit>()
    val dismiss = _dismiss

    fun onToggleMappingsButtonClick() {
        coroutineScope.launch {
            val areMappingsPaused = pauseMappings.isPaused.first()

            val serviceState = alertsUseCase.accessibilityServiceState.first()

            when {
                serviceState == ServiceState.CRASHED ->
                    if (!alertsUseCase.restartAccessibilityService()) {
                        ViewModelHelper.handleCantFindAccessibilitySettings(
                            this@HomeMenuViewModel,
                            this@HomeMenuViewModel
                        )
                    }

                serviceState == ServiceState.DISABLED ->
                    if (!alertsUseCase.startAccessibilityService()) {
                        ViewModelHelper.handleCantFindAccessibilitySettings(
                            resourceProvider = this@HomeMenuViewModel,
                            popupViewModel = this@HomeMenuViewModel
                        )
                    }

                areMappingsPaused -> pauseMappings.resume()
                !areMappingsPaused -> pauseMappings.pause()
            }

            _dismiss.emit(Unit)
        }
    }

    fun onShowInputMethodPickerClick() {
        showImePicker.show(fromForeground = true)
        runBlocking { _dismiss.emit(Unit) }
    }

    fun onOpenSettingsClick() {
        //dismiss afterwards so it is more responsive
        coroutineScope.launch {
            navigate("settings", NavDestination.Settings)
            _dismiss.emit(Unit)
        }
    }

    fun onOpenAboutClick() {
        coroutineScope.launch {
            navigate("about", NavDestination.About)
            _dismiss.emit(Unit)
        }
    }

    fun onBackupAllClick() {
        runBlocking {
            _chooseBackupFile.emit(Unit)
            _dismiss.emit(Unit)
        }
    }

    fun onRestoreClick() {
        runBlocking {
            _chooseRestoreFile.emit(Unit)
            _dismiss.emit(Unit)
        }
    }

    fun onReportBugClick() {
        coroutineScope.launch {
            navigate("report-bug", NavDestination.ReportBug)
            _dismiss.emit(Unit)
        }
    }

    fun onCreateBackupFileActivityNotFound() {
        val dialog = PopupUi.Dialog(
            message = getString(R.string.dialog_message_no_app_found_to_create_file),
            positiveButtonText = getString(R.string.pos_ok)
        )
        
        coroutineScope.launch {
            showPopup("create_document_activity_not_found", dialog)
        }
    }

    fun onChooseRestoreFileActivityNotFound() {
        val dialog = PopupUi.Dialog(
            message = getString(R.string.dialog_message_no_app_found_to_choose_a_file),
            positiveButtonText = getString(R.string.pos_ok)
        )

        coroutineScope.launch {
            showPopup("get_content_activity_not_found", dialog)
        }
    }
}

enum class ToggleMappingsButtonState {
    PAUSED, RESUMED, SERVICE_DISABLED, SERVICE_CRASHED
}