package io.github.sds100.keymapper.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 19/01/21.
 */
class SettingsViewModel(
    private val useCase: ConfigSettingsUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), PopupViewModel by PopupViewModelImpl(), ResourceProvider by resourceProvider {
    val sharedPrefsDataStoreWrapper = SharedPrefsDataStoreWrapper(useCase)

    val automaticBackupLocation = useCase.automaticBackupLocation

    val isWriteSecureSettingsPermissionGranted: StateFlow<Boolean> =
        useCase.isWriteSecureSettingsGranted
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isShizukuInstalled: StateFlow<Boolean> =
        useCase.isShizukuInstalled
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isShizukuStarted: StateFlow<Boolean> =
        useCase.isShizukuStarted
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isShizukuPermissionGranted: StateFlow<Boolean> =
        useCase.isShizukuPermissionGranted
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val rerouteKeyEvents: StateFlow<Boolean> = useCase.rerouteKeyEvents
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isCompatibleImeChosen: StateFlow<Boolean> = useCase.isCompatibleImeChosen
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isCompatibleImeEnabled: StateFlow<Boolean> = useCase.isCompatibleImeEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val defaultLongPressDelay: Flow<Int> = useCase.defaultLongPressDelay
    val defaultDoublePressDelay: Flow<Int> = useCase.defaultDoublePressDelay
    val defaultRepeatDelay: Flow<Int> = useCase.defaultRepeatDelay
    val defaultSequenceTriggerTimeout: Flow<Int> = useCase.defaultSequenceTriggerTimeout
    val defaultVibrateDuration: Flow<Int> = useCase.defaultVibrateDuration
    val defaultRepeatRate: Flow<Int> = useCase.defaultRepeatRate

    fun setAutomaticBackupLocation(uri: String) = useCase.setAutomaticBackupLocation(uri)
    fun disableAutomaticBackup() = useCase.disableAutomaticBackup()

    fun onChooseCompatibleImeClick() {
        viewModelScope.launch {
            useCase
                .chooseCompatibleIme()
                .onSuccess { ime ->
                    val snackBar =
                        PopupUi.SnackBar(
                            message = getString(
                                R.string.toast_chose_keyboard,
                                ime.label
                            )
                        )
                    showPopup("chose_ime_success", snackBar)
                }
                .otherwise {
                    useCase.showImePicker()
                }
                .onFailure { error ->
                    val snackBar =
                        PopupUi.SnackBar(message = error.getFullMessage(this@SettingsViewModel))
                    showPopup("chose_ime_error", snackBar)
                }
        }
    }

    fun onDeleteSoundFilesClick() {
        viewModelScope.launch {
            val soundFiles = useCase.getSoundFiles()

            if (soundFiles.isEmpty()) {
                showPopup("no sound files", PopupUi.Toast(getString(R.string.toast_no_sound_files)))
                return@launch
            }

            val dialog = PopupUi.MultiChoice(
                items = soundFiles.map { MultiChoiceItem(it.uid, it.name) }
            )

            val selectedFiles = showPopup("select_sound_files_to_delete", dialog) ?: return@launch

            useCase.deleteSoundFiles(selectedFiles)
        }
    }

    fun requestWriteSecureSettingsPermission() {
        useCase.requestWriteSecureSettingsPermission()
    }

    fun requestShizukuPermission() {
        useCase.requestShizukuPermission()
    }

    fun downloadShizuku() {
        useCase.downloadShizuku()
    }

    fun openShizukuApp() {
        useCase.openShizukuApp()
    }

    fun onEnableCompatibleImeClick() {
        useCase.enableCompatibleIme()
    }

    fun resetDefaultMappingOptions() {
        useCase.resetDefaultMappingOptions()
    }

    fun showNoPairedDevicesDialog() {
        viewModelScope.launch {
            val dialog = PopupUi.Dialog(
                message = getString(R.string.dialog_message_settings_no_external_devices_connected),
                positiveButtonText = getString(R.string.pos_ok)
            )

            showPopup("no_external_devices", dialog)
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val configSettingsUseCase: ConfigSettingsUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                configSettingsUseCase,
                resourceProvider
            ) as T
        }
    }
}