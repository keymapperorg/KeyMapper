package io.github.sds100.keymapper.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object Keys {
    val darkTheme = stringPreferencesKey("pref_dark_theme_mode")

    @Deprecated("Now use the libsu library to detect whether the device is rooted.")
    val hasRootPermission = booleanPreferencesKey("pref_allow_root_features")

    val shownAppIntro = booleanPreferencesKey("pref_first_time")

//    val showToggleKeyMapsNotification = booleanPreferencesKey("pref_show_remappings_notification")
//    val showToggleKeyboardNotification =
//        booleanPreferencesKey("pref_toggle_key_mapper_keyboard_notification")

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

    //    val approvedAssistantTriggerFeaturePrompt =
//        booleanPreferencesKey("pref_approved_assistant_trigger_feature_prompt")
    val approvedFloatingButtonFeaturePrompt =
        booleanPreferencesKey("pref_approved_floating_button_feature_prompt")

    val shownParallelTriggerOrderExplanation =
        booleanPreferencesKey("key_shown_parallel_trigger_order_warning")
    val shownSequenceTriggerExplanation =
        booleanPreferencesKey("key_shown_sequence_trigger_explanation_dialog")
    val lastInstalledVersionCodeHomeScreen =
        intPreferencesKey("last_installed_version_home_screen")
    val lastInstalledVersionCodeBackground =
        intPreferencesKey("last_installed_version_accessibility_service")

    val fingerprintGesturesAvailable =
        booleanPreferencesKey("fingerprint_gestures_available")

//    val rerouteKeyEvents = booleanPreferencesKey("key_reroute_key_events_from_specified_devices")
//    val devicesToRerouteKeyEvents = stringSetPreferencesKey("key_devices_to_reroute_key_events")

    val log = booleanPreferencesKey("key_log")
    val shownShizukuPermissionPrompt = booleanPreferencesKey("key_shown_shizuku_permission_prompt")
    val savedWifiSSIDs = stringSetPreferencesKey("key_saved_wifi_ssids")

    val neverShowDndAccessError = booleanPreferencesKey("key_never_show_dnd_error")
    val neverShowTriggerKeyboardIconExplanation =
        booleanPreferencesKey("key_never_show_keyboard_icon_explanation")

    val neverShowDpadImeTriggerError =
        booleanPreferencesKey("key_never_show_dpad_ime_trigger_error")
    val neverShowNoKeysRecordedError =
        booleanPreferencesKey("key_never_show_no_keys_recorded_error")
    val sortOrderJson = stringPreferencesKey("key_keymaps_sort_order_json")
    val sortShowHelp = booleanPreferencesKey("key_keymaps_sort_show_help")

    // Stored as a JSON list of ActionData objects.
    val recentlyUsedActions = stringPreferencesKey("key_recently_used_actions")

    // Stored as a JSON list of Constraint objects.
    val recentlyUsedConstraints = stringPreferencesKey("key_recently_used_constraints")

    /**
     * Whether the user viewed the advanced triggers.
     */
    val viewedAdvancedTriggers = booleanPreferencesKey("key_viewed_advanced_triggers")

    val neverShowNotificationPermissionAlert =
        booleanPreferencesKey("key_never_show_notification_permission_alert")

    val shownTapTargetCreateKeyMap =
        booleanPreferencesKey("key_shown_tap_target_create_key_map")

    val shownTapTargetChooseAction =
        booleanPreferencesKey("key_shown_tap_target_choose_action")

    val shownTapTargetChooseConstraint =
        booleanPreferencesKey("key_shown_tap_target_choose_constraint")

    val skipTapTargetTutorial =
        booleanPreferencesKey("key_skip_tap_target_tutorial")

    val isProModeWarningUnderstood =
        booleanPreferencesKey("key_is_pro_mode_warning_understood")

    val isProModeInteractiveSetupAssistantEnabled =
        booleanPreferencesKey("key_is_pro_mode_setup_assistant_enabled")

    val isProModeInfoDismissed =
        booleanPreferencesKey("key_is_pro_mode_info_dismissed")

    val isProModeAutoStartBootEnabled =
        booleanPreferencesKey("key_is_pro_mode_auto_start_boot_enabled")

    val isSystemBridgeEmergencyKilled =
        booleanPreferencesKey("key_is_system_bridge_emergency_killed")

    /**
     * Whether the user has started the system bridge before.
     */
    val isSystemBridgeUsed = booleanPreferencesKey("key_is_system_bridge_used")
}
