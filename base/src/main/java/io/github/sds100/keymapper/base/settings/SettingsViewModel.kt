package io.github.sds100.keymapper.base.settings

import android.os.Build
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.system.notifications.NotificationController
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.DialogResponse
import io.github.sds100.keymapper.base.utils.ui.MultiChoiceItem
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.otherwise
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.utils.SharedPrefsDataStoreWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val useCase: ConfigSettingsUseCase,
    private val resourceProvider: ResourceProvider,
    val sharedPrefsDataStoreWrapper: SharedPrefsDataStoreWrapper,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
) : ViewModel(),
    DialogProvider by dialogProvider,
    ResourceProvider by resourceProvider,
    NavigationProvider by navigationProvider,
    DefaultOptionsSettingsCallback {

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

    val mainScreenState: StateFlow<MainSettingsState> = combine(
        useCase.theme,
        useCase.getPreference(Keys.log),
    ) { theme, loggingEnabled ->
        MainSettingsState(
            theme = theme
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, MainSettingsState())

    val defaultSettingsScreenState: StateFlow<DefaultSettingsState> = combine(
        useCase.getPreference(Keys.defaultLongPressDelay),
        useCase.getPreference(Keys.defaultDoublePressDelay),
        useCase.getPreference(Keys.defaultVibrateDuration),
        useCase.getPreference(Keys.defaultRepeatDelay),
        useCase.getPreference(Keys.defaultRepeatRate),
        useCase.getPreference(Keys.defaultSequenceTriggerTimeout),
    ) { values ->
        DefaultSettingsState(
            longPressDelay = values[0] ?: PreferenceDefaults.LONG_PRESS_DELAY,
            defaultLongPressDelay = PreferenceDefaults.LONG_PRESS_DELAY,
            doublePressDelay = values[1] ?: PreferenceDefaults.DOUBLE_PRESS_DELAY,
            defaultDoublePressDelay = PreferenceDefaults.DOUBLE_PRESS_DELAY,
            vibrateDuration = values[2] ?: PreferenceDefaults.VIBRATION_DURATION,
            defaultVibrateDuration = PreferenceDefaults.VIBRATION_DURATION,
            repeatDelay = values[3] ?: PreferenceDefaults.REPEAT_DELAY,
            defaultRepeatDelay = PreferenceDefaults.REPEAT_DELAY,
            repeatRate = values[4] ?: PreferenceDefaults.REPEAT_RATE,
            defaultRepeatRate = PreferenceDefaults.REPEAT_RATE,
            sequenceTriggerTimeout = values[5] ?: PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT,
            defaultSequenceTriggerTimeout = PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, DefaultSettingsState())

    fun setAutomaticBackupLocation(uri: String) = useCase.setAutomaticBackupLocation(uri)
    fun disableAutomaticBackup() = useCase.disableAutomaticBackup()

    fun onChooseCompatibleImeClick() {
        viewModelScope.launch {
            useCase
                .chooseCompatibleIme()
                .onSuccess { ime ->
                    val snackBar =
                        DialogModel.SnackBar(
                            message = getString(
                                R.string.toast_chose_keyboard,
                                ime.label,
                            ),
                        )
                    showDialog("chose_ime_success", snackBar)
                }
                .otherwise {
                    useCase.showImePicker()
                }
                .onFailure { error ->
                    val snackBar =
                        DialogModel.SnackBar(message = error.getFullMessage(this@SettingsViewModel))
                    showDialog("chose_ime_error", snackBar)
                }
        }
    }

    fun onDeleteSoundFilesClick() {
        viewModelScope.launch {
            val soundFiles = useCase.getSoundFiles()

            if (soundFiles.isEmpty()) {
                showDialog(
                    "no sound files",
                    DialogModel.Toast(getString(R.string.toast_no_sound_files))
                )
                return@launch
            }

            val dialog = DialogModel.MultiChoice(
                items = soundFiles.map { MultiChoiceItem(it.uid, it.name) },
            )

            val selectedFiles = showDialog("select_sound_files_to_delete", dialog) ?: return@launch

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
                val dialog = DialogModel.Alert(
                    message = getString(R.string.dialog_message_settings_no_external_devices_connected),
                    positiveButtonText = getString(R.string.pos_ok),
                )

                showDialog("no_external_devices", dialog)
            } else {
                val checkedDevices = useCase.getPreference(prefKey).first() ?: emptySet()

                val dialog = DialogModel.MultiChoice(
                    items = externalDevices.map { device ->
                        MultiChoiceItem(
                            id = device.descriptor,
                            label = device.name,
                            isChecked = checkedDevices.contains(device.descriptor),
                        )
                    },
                )

                val newCheckedDevices = showDialog("choose_device", dialog) ?: return@launch

                useCase.setPreference(prefKey, newCheckedDevices.toSet())
            }
        }
    }

    fun onCreateBackupFileActivityNotFound() {
        val dialog = DialogModel.Alert(
            message = getString(R.string.dialog_message_no_app_found_to_create_file),
            positiveButtonText = getString(R.string.pos_ok),
        )

        viewModelScope.launch {
            showDialog("create_document_activity_not_found", dialog)
        }
    }

    fun onResetAllSettingsClick() {
        val dialog = DialogModel.Alert(
            title = getString(R.string.dialog_title_reset_settings),
            message = getString(R.string.dialog_message_reset_settings),
            positiveButtonText = getString(R.string.pos_button_reset_settings),
            negativeButtonText = getString(R.string.neg_cancel),
        )

        viewModelScope.launch {
            val response = showDialog("reset_settings_dialog", dialog)

            if (response == DialogResponse.POSITIVE) {
                useCase.resetAllSettings()
            }
        }
    }

    fun onRequestRootClick() {
        useCase.requestRootPermission()
    }

    fun onProModeClick() {
        viewModelScope.launch {
            navigate("pro_mode_settings", NavDestination.ProMode)
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            popBackStack()
        }
    }

    fun onThemeSelected(theme: Theme) {
        viewModelScope.launch {
            useCase.setPreference(Keys.darkTheme, theme.value.toString())
        }
    }

    fun onPauseResumeNotificationClick() {
        onNotificationSettingsClick(NotificationController.CHANNEL_TOGGLE_KEYMAPS)
    }

    fun onDefaultOptionsClick() {
        viewModelScope.launch {
            navigate("default_options", NavDestination.DefaultOptionsSettings)
        }
    }

    override fun onLongPressDelayChanged(delay: Int) {
        viewModelScope.launch {
            useCase.setPreference(Keys.defaultLongPressDelay, delay)
        }
    }

    override fun onDoublePressDelayChanged(delay: Int) {
        viewModelScope.launch {
            useCase.setPreference(Keys.defaultDoublePressDelay, delay)
        }
    }

    override fun onVibrateDurationChanged(duration: Int) {
        viewModelScope.launch {
            useCase.setPreference(Keys.defaultVibrateDuration, duration)
        }
    }

    override fun onRepeatDelayChanged(delay: Int) {
        viewModelScope.launch {
            useCase.setPreference(Keys.defaultRepeatDelay, delay)
        }
    }

    override fun onRepeatRateChanged(rate: Int) {
        viewModelScope.launch {
            useCase.setPreference(Keys.defaultRepeatRate, rate)
        }
    }

    override fun onSequenceTriggerTimeoutChanged(timeout: Int) {
        viewModelScope.launch {
            useCase.setPreference(Keys.defaultSequenceTriggerTimeout, timeout)
        }
    }

    private fun onNotificationSettingsClick(channel: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !useCase.isNotificationsPermissionGranted()
        ) {
            useCase.requestNotificationsPermission()
            return
        }

        useCase.openNotificationChannelSettings(channel)
    }
}

data class MainSettingsState(
    val theme: Theme = Theme.AUTO
)

data class DefaultSettingsState(
    val longPressDelay: Int = PreferenceDefaults.LONG_PRESS_DELAY,
    val defaultLongPressDelay: Int = PreferenceDefaults.LONG_PRESS_DELAY,

    val doublePressDelay: Int = PreferenceDefaults.DOUBLE_PRESS_DELAY,
    val defaultDoublePressDelay: Int = PreferenceDefaults.DOUBLE_PRESS_DELAY,

    val vibrateDuration: Int = PreferenceDefaults.VIBRATION_DURATION,
    val defaultVibrateDuration: Int = PreferenceDefaults.VIBRATION_DURATION,

    val repeatDelay: Int = PreferenceDefaults.REPEAT_DELAY,
    val defaultRepeatDelay: Int = PreferenceDefaults.REPEAT_DELAY,

    val repeatRate: Int = PreferenceDefaults.REPEAT_RATE,
    val defaultRepeatRate: Int = PreferenceDefaults.REPEAT_RATE,

    val sequenceTriggerTimeout: Int = PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT,
    val defaultSequenceTriggerTimeout: Int = PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT,
)