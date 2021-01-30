package io.github.sds100.keymapper.data

import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.preferencesSetKey

/**
 * Created by sds100 on 19/01/21.
 */
object Keys {
    val darkTheme = preferencesKey<String>("pref_dark_theme_mode")
    val hasRootPermission = preferencesKey<Boolean>("pref_allow_root_features")
    val shownAppIntro = preferencesKey<Boolean>("pref_first_time")
    val showImePickerNotification = preferencesKey<Boolean>("pref_show_ime_notification")
    val showToggleKeymapsNotification = preferencesKey<Boolean>("pref_show_remappings_notification")
    val showToggleKeyboardNotification = preferencesKey<Boolean>("pref_toggle_key_mapper_keyboard_notification")
    val bluetoothDevicesThatToggleKeymaps = preferencesSetKey<String>("pref_bluetooth_devices")
    val autoChangeImeOnBtConnect = preferencesKey<Boolean>("pref_auto_change_ime_on_connect_disconnect")
    val autoShowImePicker = preferencesKey<Boolean>("pref_auto_show_ime_picker")
    val longPressDelay = preferencesKey<Int>("pref_long_press_delay")
    val doublePressDelay = preferencesKey<Int>("pref_double_press_delay")
    val forceVibrate = preferencesKey<Boolean>("pref_force_vibrate")
    val vibrateDuration = preferencesKey<Int>("pref_vibrate_duration")
    val repeatDelay = preferencesKey<Int>("pref_hold_down_delay")
    val repeatRate = preferencesKey<Int>("pref_repeat_delay")
    val sequenceTriggerTimeout = preferencesKey<Int>("pref_sequence_trigger_timeout")
    val toggleKeyboardOnToggleKeymaps = preferencesKey<Boolean>("key_toggle_keyboard_on_pause_resume_keymaps")
    val automaticBackupLocation = preferencesKey<String>("pref_automatic_backup_location")
    val keymapsPaused = preferencesKey<Boolean>("pref_keymaps_paused")
    val hideHomeScreenAlerts = preferencesKey<Boolean>("pref_hide_home_screen_alerts")
    val showGuiKeyboardAd = preferencesKey<Boolean>("pref_show_gui_keyboard_ad")
    val showDeviceDescriptors = preferencesKey<Boolean>("pref_show_device_descriptors")
    val holdDownDuration = preferencesKey<Int>("pref_hold_down_duration")
    val approvedFingerprintFeaturePrompt = preferencesKey<Boolean>("pref_approved_fingerprint_feature_prompt")

    val shownScreenOffTriggersExplanation = preferencesKey<Boolean>("pref_screen_off_triggers_explanation")
    val shownParallelTriggerOrderExplanation = preferencesKey<Boolean>("key_shown_parallel_trigger_order_warning")
    val shownSequenceTriggerExplanation = preferencesKey<Boolean>("key_shown_sequence_trigger_explanation_dialog")
    val shownMultipleOfSameKeyInSequenceTriggerExplanation = preferencesKey<Boolean>("pref_shown_multiple_of_same_key_in_sequence_trigger_info")
}