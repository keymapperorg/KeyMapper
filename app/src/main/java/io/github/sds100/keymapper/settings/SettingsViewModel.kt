package io.github.sds100.keymapper.settings

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.common.result.onFailure
import io.github.sds100.keymapper.common.result.onSuccess
import io.github.sds100.keymapper.common.result.otherwise
import io.github.sds100.keymapper.util.SharedPrefsDataStoreWrapper
import io.github.sds100.keymapper.common.state.State
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.ui.DialogResponse
import io.github.sds100.keymapper.util.ui.MultiChoiceItem
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class SettingsViewModel(
    private val useCase: ConfigSettingsUseCase,
    resourceProvider: ResourceProvider,
) : ViewModel(),
    PopupViewModel by PopupViewModelImpl(),
    ResourceProvider by resourceProvider {
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
                                ime.label,
                            ),
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
                items = soundFiles.map { MultiChoiceItem(it.uid, it.name) },
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

    fun isNotificationPermissionGranted(): Boolean = useCase.isNotificationsPermissionGranted()

    fun requestNotificationsPermission() {
        useCase.requestNotificationsPermission()
    }

    fun onEnableCompatibleImeClick() {
        viewModelScope.launch {
            useCase.enableCompatibleIme()
        }
    }

    fun resetDefaultMappingOptions() {
        useCase.resetDefaultMappingOptions()
    }

    fun chooseDevicesForPreference(prefKey: Preferences.Key<Set<String>>) {
        viewModelScope.launch {
            val externalDevices = useCase.connectedInputDevices
                .first { it is State.Data<*> }
                .let { (it as State.Data).data }
                .filter { it.isExternal }

            if (externalDevices.isEmpty()) {
                val dialog = PopupUi.Dialog(
                    message = getString(R.string.dialog_message_settings_no_external_devices_connected),
                    positiveButtonText = getString(R.string.pos_ok),
                )

                showPopup("no_external_devices", dialog)
            } else {
                val checkedDevices = useCase.getPreference(prefKey).first() ?: emptySet()

                val dialog = PopupUi.MultiChoice(
                    items = externalDevices.map { device ->
                        MultiChoiceItem(
                            id = device.descriptor,
                            label = device.name,
                            isChecked = checkedDevices.contains(device.descriptor),
                        )
                    },
                )

                val newCheckedDevices = showPopup("choose_device", dialog) ?: return@launch

                useCase.setPreference(prefKey, newCheckedDevices.toSet())
            }
        }
    }

    fun onCreateBackupFileActivityNotFound() {
        val dialog = PopupUi.Dialog(
            message = getString(R.string.dialog_message_no_app_found_to_create_file),
            positiveButtonText = getString(R.string.pos_ok),
        )

        viewModelScope.launch {
            showPopup("create_document_activity_not_found", dialog)
        }
    }

    fun onResetAllSettingsClick() {
        val dialog = PopupUi.Dialog(
            title = getString(R.string.dialog_title_reset_settings),
            message = getString(R.string.dialog_message_reset_settings),
            positiveButtonText = getString(R.string.pos_button_reset_settings),
            negativeButtonText = getString(R.string.neg_cancel),
        )

        viewModelScope.launch {
            val response = showPopup("reset_settings_dialog", dialog)

            if (response == DialogResponse.POSITIVE) {
                useCase.resetAllSettings()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val configSettingsUseCase: ConfigSettingsUseCase,
        private val resourceProvider: ResourceProvider,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(
            configSettingsUseCase,
            resourceProvider,
        ) as T
    }
}
