package io.github.sds100.keymapper.data

import androidx.appcompat.app.AppCompatDelegate.*
import io.github.sds100.keymapper.R
import splitties.preferences.DefaultPreferences
import splitties.resources.appBool
import splitties.resources.appInt
import splitties.resources.appStr

/**
 * Created by sds100 on 20/02/2020.
 */

@Suppress("EXPERIMENTAL_API_USAGE")
object AppPreferences : DefaultPreferences() {
    private const val KEY_DEFAULT_IME = "key_default_ime"

    private var mDarkThemeModePref by StringPref(
        appStr(R.string.key_pref_dark_theme_mode),
        appStr(R.string.default_value_dark_theme_mode)
    )

    @NightMode
    var darkThemeMode: Int
        get() = getSdkNightMode(mDarkThemeModePref)
        set(value) {
            when (value) {
                MODE_NIGHT_YES -> mDarkThemeModePref = appStr(R.string.value_pref_dark_theme_enabled)
                MODE_NIGHT_NO -> mDarkThemeModePref = appStr(R.string.value_pref_dark_theme_disabled)
                MODE_NIGHT_FOLLOW_SYSTEM -> mDarkThemeModePref = appStr(R.string.value_pref_dark_theme_follow_system)
            }
        }

    val hasRootPermission by BoolPref(
        appStr(R.string.key_pref_root_permission),
        appBool(R.bool.default_value_root_permission)
    )

    var shownKeyMapperImeWarningDialog by BoolPref(
        appStr(R.string.key_pref_shown_cant_use_virtual_keyboard_message),
        appBool(R.bool.default_value_shown_cant_use_virtual_keyboard_message)
    )

    var showImePickerNotification by BoolPref(
        appStr(R.string.key_pref_show_ime_notification),
        appBool(R.bool.default_value_show_ime_notification)
    )

    var showToggleRemapsNotification by BoolPref(
        appStr(R.string.key_pref_show_toggle_remappings_notification),
        appBool(R.bool.default_value_show_toggle_remappings_notification)
    )

    var bluetoothDevices by StringSetOrNullPref(
        appStr(R.string.key_pref_bluetooth_devices)
    )

    var autoChangeImeOnBtConnect by BoolPref(
        appStr(R.string.key_pref_auto_change_ime_on_connection),
        appBool(R.bool.default_value_auto_change_ime_on_connection)
    )

    var autoShowImePicker by BoolPref(
        appStr(R.string.key_pref_auto_show_ime_picker),
        appBool(R.bool.default_value_auto_show_ime_picker)
    )

    var shownDoublePressRestrictionWarning by BoolPref(
        appStr(R.string.key_pref_shown_double_press_restriction_warning),
        false
    )

    var shownParallelTriggerOrderDialog by BoolPref(
        appStr(R.string.key_pref_shown_parallel_trigger_order_dialog),
        false
    )

    var shownSequenceTriggerExplanationDialog by BoolPref(
        appStr(R.string.key_pref_shown_sequence_trigger_explanation_dialog),
        false
    )

    var defaultIme by StringOrNullPref(KEY_DEFAULT_IME)

    val longPressDelay by IntPref(
        appStr(R.string.key_pref_long_press_delay),
        appInt(R.integer.default_value_long_press_delay)
    )

    val doublePressDelay by IntPref(
        appStr(R.string.key_pref_double_press_delay),
        appInt(R.integer.default_value_double_press_delay)
    )

    @NightMode
    fun getSdkNightMode(darkThemePrefValue: String): Int {
        return when (darkThemePrefValue) {
            appStr(R.string.value_pref_dark_theme_enabled) -> MODE_NIGHT_YES
            appStr(R.string.value_pref_dark_theme_disabled) -> MODE_NIGHT_NO
            appStr(R.string.value_pref_dark_theme_follow_system) -> MODE_NIGHT_FOLLOW_SYSTEM
            else -> MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}