package io.github.sds100.keymapper.home

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
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
) : ResourceProvider by resourceProvider, PopupViewModel by PopupViewModelImpl() {

    val toggleMappingsButtonState: StateFlow<ToggleMappingsButtonState?> =
        combine(
            pauseMappings.isPaused,
            alertsUseCase.serviceState
        ) { isPaused, serviceState ->
            val text = when (serviceState) {
                ServiceState.ENABLED ->
                    if (isPaused) {
                        getString(R.string.action_tap_to_resume_keymaps)
                    } else {
                        getString(R.string.action_tap_to_pause_keymaps)
                    }
                ServiceState.CRASHED -> getString(R.string.button_restart_accessibility_service)
                ServiceState.DISABLED -> getString(R.string.button_enable_accessibility_service)
            }

            val tint = when {
                serviceState != ServiceState.ENABLED || !isPaused -> getColor(R.color.slideRed)
                else -> getColor(R.color.slideGreen)
            }

            ToggleMappingsButtonState(text, tint)

        }.stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private val _openSettings = MutableSharedFlow<Unit>()
    val openSettings = _openSettings.asSharedFlow()

    private val _openAbout = MutableSharedFlow<Unit>()
    val openAbout = _openAbout.asSharedFlow()

    private val _openUrl = MutableSharedFlow<String>()
    val openUrl = _openUrl.asSharedFlow()

    private val _chooseBackupFile = MutableSharedFlow<Unit>()
    val chooseBackupFile = _chooseBackupFile.asSharedFlow()

    private val _chooseRestoreFile = MutableSharedFlow<Unit>()
    val chooseRestoreFile = _chooseRestoreFile.asSharedFlow()

    private val _dismiss = MutableSharedFlow<Unit>()
    val dismiss = _dismiss

    private val _reportBug = MutableSharedFlow<Unit>()
    val reportBug = _reportBug.asSharedFlow()

    fun onToggleMappingsButtonClick() {
        coroutineScope.launch {
            val areMappingsPaused = pauseMappings.isPaused.first()

            when {
                alertsUseCase.serviceState.first() == ServiceState.CRASHED -> alertsUseCase.restartAccessibilityService()
                alertsUseCase.serviceState.first() == ServiceState.DISABLED -> alertsUseCase.enableAccessibilityService()
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
        runBlocking { _openSettings.emit(Unit) }
        runBlocking { _dismiss.emit(Unit) }
    }

    fun onOpenAboutClick() {
        runBlocking { _openAbout.emit(Unit) }
        runBlocking { _dismiss.emit(Unit) }
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
            _reportBug.emit(Unit)
        }
    }
}

data class ToggleMappingsButtonState(
    val text: String,
    val tint: Int
)