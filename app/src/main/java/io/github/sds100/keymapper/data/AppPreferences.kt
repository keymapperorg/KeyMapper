package io.github.sds100.keymapper.data

import androidx.appcompat.app.AppCompatDelegate.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.bool
import io.github.sds100.keymapper.util.int
import io.github.sds100.keymapper.util.str
import splitties.init.appCtx
import splitties.preferences.DefaultPreferences

/**
 * Created by sds100 on 20/02/2020.
 */

@Suppress("EXPERIMENTAL_API_USAGE")
object AppPreferences : DefaultPreferences() {
    private const val KEY_DEFAULT_IME = "key_default_ime"

    private var mDarkThemeModePref by StringPref(
        appCtx.str(R.string.key_pref_dark_theme_mode),
        appCtx.str(R.string.default_value_dark_theme_mode)
    )

    @NightMode
    var darkThemeMode: Int
        get() = getSdkNightMode(mDarkThemeModePref)
        set(value) {
            when (value) {
                MODE_NIGHT_YES -> mDarkThemeModePref = appCtx.str(R.string.value_pref_dark_theme_enabled)
                MODE_NIGHT_NO -> mDarkThemeModePref = appCtx.str(R.string.value_pref_dark_theme_disabled)
                MODE_NIGHT_FOLLOW_SYSTEM -> mDarkThemeModePref = appCtx.str(R.string.value_pref_dark_theme_follow_system)
            }
        }

    val hasRootPermission by BoolPref(
        appCtx.str(R.string.key_pref_root_permission),
        appCtx.bool(R.bool.default_value_root_permission)
    )

    var showImePickerNotification by BoolPref(
        appCtx.str(R.string.key_pref_show_ime_notification),
        appCtx.bool(R.bool.default_value_show_ime_notification)
    )

    var showToggleRemapsNotification by BoolPref(
        appCtx.str(R.string.key_pref_show_toggle_remappings_notification),
        appCtx.bool(R.bool.default_value_show_toggle_remappings_notification)
    )

    var showToggleKeyboardNotification by BoolPref(
        appCtx.str(R.string.key_pref_toggle_keyboard_notification),
        appCtx.bool(R.bool.default_value_toggle_keyboard_notification)
    )

    var bluetoothDevices by StringSetOrNullPref(
        appCtx.str(R.string.key_pref_bluetooth_devices)
    )

    var autoChangeImeOnBtConnect by BoolPref(
        appCtx.str(R.string.key_pref_auto_change_ime_on_connection),
        appCtx.bool(R.bool.default_value_auto_change_ime_on_connection)
    )

    var autoShowImePicker by BoolPref(
        appCtx.str(R.string.key_pref_auto_show_ime_picker),
        appCtx.bool(R.bool.default_value_auto_show_ime_picker)
    )

    var defaultIme by StringOrNullPref(KEY_DEFAULT_IME)

    val longPressDelay by IntPref(
        appCtx.str(R.string.key_pref_long_press_delay),
        appCtx.int(R.integer.default_value_long_press_delay)
    )

    val doublePressDelay by IntPref(
        appCtx.str(R.string.key_pref_double_press_delay),
        appCtx.int(R.integer.default_value_double_press_delay)
    )

    val forceVibrate by BoolPref(
        appCtx.str(R.string.key_pref_force_vibrate),
        appCtx.bool(R.bool.default_value_force_vibrate)
    )

    val vibrateDuration by IntPref(
        appCtx.str(R.string.key_pref_vibrate_duration),
        appCtx.int(R.integer.default_value_vibrate_duration)
    )

    val holdDownDelay by IntPref(
        appCtx.str(R.string.key_pref_hold_down_delay),
        appCtx.int(R.integer.default_value_vibrate_duration)
    )

    val repeatDelay by IntPref(
        appCtx.str(R.string.key_pref_repeat_delay),
        appCtx.int(R.integer.default_value_repeat_delay)
    )

    val sequenceTriggerTimeout by IntPref(
        appCtx.str(R.string.key_pref_sequence_trigger_timeout),
        appCtx.int(R.integer.default_value_sequence_trigger_timeout)
    )

    val showToastOnActionError by BoolPref(
        appCtx.str(R.string.key_pref_show_toast_on_action_error),
        appCtx.bool(R.bool.default_value_show_toast_on_action_error)
    )

    var shownAppIntro by BoolPref(
        appCtx.str(R.string.key_pref_first_time),
        false
    )

    var lastInstalledVersionCode by IntPref(
        appCtx.str(R.string.key_pref_last_installed_version),
        -1
    )

    val toggleKeyboardOnToggleKeymaps by BoolPref(
        appCtx.str(R.string.key_pref_toggle_keyboard_on_toggle_keymaps),
        appCtx.bool(R.bool.default_value_toggle_keyboard_on_toggle_keymaps)
    )

    var automaticBackupLocation by StringPref(
        appCtx.str(R.string.key_pref_automatic_backup_location),
        appCtx.str(R.string.default_value_automatic_backup_location)
    )

    var keymapsPaused by BoolPref(
        appCtx.str(R.string.key_pref_keymaps_paused),
        appCtx.bool(R.bool.default_value_keymaps_paused)
    )

    @NightMode
    fun getSdkNightMode(darkThemePrefValue: String): Int {
        return when (darkThemePrefValue) {
            appCtx.str(R.string.value_pref_dark_theme_enabled) -> MODE_NIGHT_YES
            appCtx.str(R.string.value_pref_dark_theme_disabled) -> MODE_NIGHT_NO
            appCtx.str(R.string.value_pref_dark_theme_follow_system) -> MODE_NIGHT_FOLLOW_SYSTEM
            else -> MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}