package io.github.sds100.keymapper.base.settings

import android.os.Build
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.logging.ShareLogcatUseCase
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
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val useCase: ConfigSettingsUseCase,
    private val resourceProvider: ResourceProvider,
    private val shareLogcatUseCase: ShareLogcatUseCase,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
) : ViewModel(),
    DialogProvider by dialogProvider,
    ResourceProvider by resourceProvider,
    NavigationProvider by navigationProvider,
    DefaultOptionsSettingsCallback {

    val defaultLongPressDelay: Flow<Int> = useCase.defaultLongPressDelay
    val defaultDoublePressDelay: Flow<Int> = useCase.defaultDoublePressDelay
    val defaultRepeatDelay: Flow<Int> = useCase.defaultRepeatDelay
    val defaultSequenceTriggerTimeout: Flow<Int> = useCase.defaultSequenceTriggerTimeout
    val defaultVibrateDuration: Flow<Int> = useCase.defaultVibrateDuration
    val defaultRepeatRate: Flow<Int> = useCase.defaultRepeatRate

    val mainScreenState: StateFlow<MainSettingsState> = combine(
        useCase.theme,
        useCase.getPreference(Keys.log),
        useCase.automaticBackupLocation,
        useCase.getPreference(Keys.forceVibrate),
        useCase.getPreference(Keys.hideHomeScreenAlerts),
        useCase.getPreference(Keys.showDeviceDescriptors),
        useCase.getPreference(Keys.keyEventActionsUseSystemBridge),
    ) { values ->
        MainSettingsState(
            theme = values[0] as Theme,
            loggingEnabled = values[1] as Boolean? ?: false,
            autoBackupLocation = values[2] as String?,
            forceVibrate = values[3] as Boolean? ?: false,
            hideHomeScreenAlerts = values[4] as Boolean? ?: false,
            showDeviceDescriptors = values[5] as Boolean? ?: false,
            keyEventActionsUseSystemBridge = values[6] as Boolean? ?: false,
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

    val automaticChangeImeSettingsState: StateFlow<AutomaticChangeImeSettingsState> = combine(
        useCase.getPreference(Keys.showToastWhenAutoChangingIme),
        useCase.getPreference(Keys.changeImeOnInputFocus),
        useCase.getPreference(Keys.changeImeOnDeviceConnect),
        useCase.getPreference(Keys.toggleKeyboardOnToggleKeymaps),
    ) { values ->
        AutomaticChangeImeSettingsState(
            showToastWhenAutoChangingIme = values[0]
                ?: PreferenceDefaults.SHOW_TOAST_WHEN_AUTO_CHANGE_IME,
            changeImeOnInputFocus = values[1] ?: PreferenceDefaults.CHANGE_IME_ON_INPUT_FOCUS,
            changeImeOnDeviceConnect = values[2] ?: false,
            toggleKeyboardOnToggleKeymaps = values[3] ?: false,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, AutomaticChangeImeSettingsState())

    fun setAutomaticBackupLocation(uri: String) = useCase.setAutomaticBackupLocation(uri)

    fun disableAutomaticBackup() = useCase.disableAutomaticBackup()

    fun chooseDevicesForPreference(prefKey: Preferences.Key<Set<String>>) {
        viewModelScope.launch {
            val externalDevices = useCase.connectedInputDevices
                .first { it is State.Data<*> }
                .let { (it as State.Data).data }
                .filter { it.isExternal }

            if (externalDevices.isEmpty()) {
                val dialog = DialogModel.Alert(
                    message = getString(
                        R.string.dialog_message_settings_no_external_devices_connected,
                    ),
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

    fun onAutomaticChangeImeClick() {
        viewModelScope.launch {
            navigate("automatic_change_ime", NavDestination.AutomaticChangeImeSettings)
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
        onNotificationSettingsClick(NotificationController.CHANNEL_TOGGLE_KEY_MAPS)
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

    fun onShowToastWhenAutoChangingImeToggled(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setPreference(Keys.showToastWhenAutoChangingIme, enabled)
        }
    }

    fun onChangeImeOnInputFocusToggled(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setPreference(Keys.changeImeOnInputFocus, enabled)
        }
    }

    fun onChangeImeOnDeviceConnectToggled(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setPreference(Keys.changeImeOnDeviceConnect, enabled)
        }
    }

    fun onDevicesThatChangeImeClick() {
        chooseDevicesForPreference(Keys.devicesThatChangeIme)
    }

    fun onToggleKeyboardOnToggleKeymapsToggled(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setPreference(Keys.toggleKeyboardOnToggleKeymaps, enabled)
        }
    }

    fun onShowToggleKeyboardNotificationClick() {
        onNotificationSettingsClick(NotificationController.CHANNEL_TOGGLE_KEYBOARD)
    }

    fun onForceVibrateToggled(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setPreference(Keys.forceVibrate, enabled)
        }
    }

    fun onLoggingToggled(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setPreference(Keys.log, enabled)
        }
    }

    fun onViewLogClick() {
        viewModelScope.launch {
            navigate("log", NavDestination.Log)
        }
    }

    fun onShareLogcatClick() {
        viewModelScope.launch {
            if (shareLogcatUseCase.isPermissionGranted()) {
                shareLogcatUseCase.share().onFailure { error ->
                    val dialog = DialogModel.Ok(
                        title = getString(R.string.dialog_title_share_logcat_error),
                        message = error.getFullMessage(this@SettingsViewModel),
                    )
                    showDialog("logcat_error", dialog)
                }
            } else {
                val dialog = DialogModel.Alert(
                    title = getString(R.string.dialog_title_grant_read_logs_permission_title),
                    message = getString(R.string.dialog_title_grant_read_logs_permission_message),
                    positiveButtonText = getString(R.string.pos_proceed),
                    negativeButtonText = getString(R.string.neg_cancel),
                )

                val response = showDialog("read_logs_permission", dialog)

                if (response == DialogResponse.POSITIVE) {
                    shareLogcatUseCase.grantPermission().onFailure { error ->
                        val dialog = DialogModel.Ok(
                            message = error.getFullMessage(resourceProvider),
                        )
                        showDialog("grant_read_log_failure", dialog)
                    }
                }
            }
        }
    }

    fun onKeyEventActionMethodSelected(isProModeSelected: Boolean) {
        viewModelScope.launch {
            useCase.setPreference(Keys.keyEventActionsUseSystemBridge, isProModeSelected)
        }
    }

    fun onHideHomeScreenAlertsToggled(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setPreference(Keys.hideHomeScreenAlerts, enabled)
        }
    }

    fun onShowDeviceDescriptorsToggled(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setPreference(Keys.showDeviceDescriptors, enabled)
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
    val theme: Theme = Theme.AUTO,
    val autoBackupLocation: String? = null,
    val forceVibrate: Boolean = false,
    val loggingEnabled: Boolean = false,
    val hideHomeScreenAlerts: Boolean = false,
    val showDeviceDescriptors: Boolean = false,
    val keyEventActionsUseSystemBridge: Boolean = false,
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

data class AutomaticChangeImeSettingsState(
    val showToastWhenAutoChangingIme: Boolean = PreferenceDefaults.SHOW_TOAST_WHEN_AUTO_CHANGE_IME,
    val changeImeOnInputFocus: Boolean = PreferenceDefaults.CHANGE_IME_ON_INPUT_FOCUS,
    val changeImeOnDeviceConnect: Boolean = false,
    val toggleKeyboardOnToggleKeymaps: Boolean = false,
)
