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
    val showToggleKeyMapsNotification = booleanPreferencesKey("pref_show_remappings_notification")
    val showToggleKeyboardNotification =
        booleanPreferencesKey("pref_toggle_key_mapper_keyboard_notification")

    val devicesThatChangeIme = stringSetPreferencesKey("pref_devices_that_change_ime")
    val changeImeOnDeviceConnect =
        booleanPreferencesKey("pref_auto_change_ime_on_connect_disconnect")
    val changeImeOnInputFocus = booleanPreferencesKey("pref_change_ime_on_input_focus")
    val showToastWhenAutoChangingIme =
        booleanPreferencesKey("pref_show_toast_when_auto_changing_ime")

    val devicesThatShowImePicker = stringSetPreferencesKey("pref_devices_show_ime_picker")
    val showImePickerOnDeviceConnect = booleanPreferencesKey("pref_auto_show_ime_picker")

    val forceVibrate = booleanPreferencesKey("pref_force_vibrate")
    val defaultLongPressDelay = intPreferencesKey("pref_long_press_delay")
    val defaultDoublePressDelay = intPreferencesKey("pref_double_press_delay")
    val defaultVibrateDuration = intPreferencesKey("pref_vibrate_duration")
    val defaultRepeatDelay = intPreferencesKey("pref_hold_down_delay")
    val defaultRepeatRate = intPreferencesKey("pref_repeat_delay")
    val defaultSequenceTriggerTimeout = intPreferencesKey("pref_sequence_trigger_timeout")
    val defaultHoldDownDuration = intPreferencesKey("pref_hold_down_duration")
    val toggleKeyboardOnToggleKeymaps =
        booleanPreferencesKey("key_toggle_keyboard_on_pause_resume_keymaps")
    val automaticBackupLocation = stringPreferencesKey("pref_automatic_backup_location")
    val mappingsPaused = booleanPreferencesKey("pref_keymaps_paused")
    val hideHomeScreenAlerts = booleanPreferencesKey("pref_hide_home_screen_alerts")
    val acknowledgedGuiKeyboard = booleanPreferencesKey("pref_acknowledged_gui_keyboard")
    val showDeviceDescriptors = booleanPreferencesKey("pref_show_device_descriptors")
    val approvedFingerprintFeaturePrompt =
        booleanPreferencesKey("pref_approved_fingerprint_feature_prompt")
    val shownParallelTriggerOrderExplanation =
        booleanPreferencesKey("key_shown_parallel_trigger_order_warning")
    val shownSequenceTriggerExplanation =
        booleanPreferencesKey("key_shown_sequence_trigger_explanation_dialog")
    val lastInstalledVersionCodeHomeScreen =
        intPreferencesKey("last_installed_version_home_screen")
    val lastInstalledVersionCodeBackground =
        intPreferencesKey("last_installed_version_accessibility_service")

    val shownQuickStartGuideHint = booleanPreferencesKey("tap_target_quick_start_guide")

    val fingerprintGesturesAvailable =
        booleanPreferencesKey("fingerprint_gestures_available")

    val approvedSetupChosenDevicesAgain =
        booleanPreferencesKey("pref_approved_new_choose_devices_settings")

    val rerouteKeyEvents = booleanPreferencesKey("key_reroute_key_events_from_specified_devices")
    val devicesToRerouteKeyEvents =
        stringSetPreferencesKey("key_devices_to_reroute_key_events")

    val log = booleanPreferencesKey("key_log")
    val shownShizukuPermissionPrompt = booleanPreferencesKey("key_shown_shizuku_permission_prompt")
    val savedWifiSSIDs = stringSetPreferencesKey("key_saved_wifi_ssids")

    val neverShowDndError = booleanPreferencesKey("key_never_show_dnd_error")
}
