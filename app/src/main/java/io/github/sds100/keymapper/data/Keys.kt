package io.github.sds100.keymapper.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * Created by sds100 on 19/01/21.
 */
object Keys {
    val darkTheme = stringPreferencesKey("pref_dark_theme_mode")
    val hasRootPermission = booleanPreferencesKey("pref_allow_root_features")
    val shownAppIntro = booleanPreferencesKey("pref_first_time")
    val showImePickerNotification = booleanPreferencesKey("pref_show_ime_notification")
    val showToggleKeymapsNotification = booleanPreferencesKey("pref_show_remappings_notification")
    val showToggleKeyboardNotification =
        booleanPreferencesKey("pref_toggle_key_mapper_keyboard_notification")

    val autoChangeImeOnDeviceConnect =
        booleanPreferencesKey("pref_auto_change_ime_on_connect_disconnect")

    @Deprecated(message = "Replaced with devicesThatToggleKeyboard")
    val bluetoothDevicesThatToggleKeyboard = stringSetPreferencesKey("pref_bluetooth_devices")
    val devicesThatToggleKeyboard = stringSetPreferencesKey("pref_bluetooth_devices")

    val autoShowImePickerOnDeviceConnect = booleanPreferencesKey("pref_auto_show_ime_picker")

    @Deprecated(message = "Replaced with devicesThatShowImePicker")
    val bluetoothDevicesThatShowImePicker =
        stringSetPreferencesKey("pref_bluetooth_devices_show_ime_picker")
    val devicesThatShowImePicker =
        stringSetPreferencesKey("pref_bluetooth_devices_show_ime_picker")

    val longPressDelay = intPreferencesKey("pref_long_press_delay")
    val doublePressDelay = intPreferencesKey("pref_double_press_delay")
    val forceVibrate = booleanPreferencesKey("pref_force_vibrate")
    val vibrateDuration = intPreferencesKey("pref_vibrate_duration")
    val repeatDelay = intPreferencesKey("pref_hold_down_delay")
    val repeatRate = intPreferencesKey("pref_repeat_delay")
    val sequenceTriggerTimeout = intPreferencesKey("pref_sequence_trigger_timeout")
    val toggleKeyboardOnToggleKeymaps =
        booleanPreferencesKey("key_toggle_keyboard_on_pause_resume_keymaps")
    val automaticBackupLocation = stringPreferencesKey("pref_automatic_backup_location")
    val keymapsPaused = booleanPreferencesKey("pref_keymaps_paused")
    val hideHomeScreenAlerts = booleanPreferencesKey("pref_hide_home_screen_alerts")
    val showGuiKeyboardAd = booleanPreferencesKey("pref_show_gui_keyboard_ad")
    val showDeviceDescriptors = booleanPreferencesKey("pref_show_device_descriptors")
    val holdDownDuration = intPreferencesKey("pref_hold_down_duration")
    val approvedFingerprintFeaturePrompt =
        booleanPreferencesKey("pref_approved_fingerprint_feature_prompt")
    val approvedSetupChosenDevicesAgain =
        booleanPreferencesKey("pref_approved_new_choose_devices_settings")

    val shownScreenOffTriggersExplanation =
        booleanPreferencesKey("pref_screen_off_triggers_explanation")
    val shownParallelTriggerOrderExplanation =
        booleanPreferencesKey("key_shown_parallel_trigger_order_warning")
    val shownSequenceTriggerExplanation =
        booleanPreferencesKey("key_shown_sequence_trigger_explanation_dialog")
}