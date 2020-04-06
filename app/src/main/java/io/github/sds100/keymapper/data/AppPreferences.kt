package io.github.sds100.keymapper.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.core.app.NotificationManagerCompat
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.NotificationUtils
import splitties.preferences.DefaultPreferences
import splitties.resources.appBool
import splitties.resources.appInt
import splitties.resources.appStr
import splitties.systemservices.notificationManager

/**
 * Created by sds100 on 20/02/2020.
 */

@Suppress("EXPERIMENTAL_API_USAGE")
object AppPreferences : DefaultPreferences() {
    private const val KEY_DEFAULT_IME = "key_default_ime"

    private var mDarkThemeModePref by IntPref(
        appStr(R.string.key_pref_dark_theme_mode),
        appInt(R.integer.default_value_dark_theme_mode)
    )

    @NightMode
    var darkThemeMode: Int
        get() = when (mDarkThemeModePref) {
            appInt(R.integer.value_pref_dark_theme_enabled) -> MODE_NIGHT_YES
            appInt(R.integer.value_pref_dark_theme_disabled) -> MODE_NIGHT_NO
            appInt(R.integer.value_pref_dark_theme_follow_system) -> MODE_NIGHT_FOLLOW_SYSTEM
            else -> MODE_NIGHT_FOLLOW_SYSTEM
        }
        set(value) {
            when (value) {
                MODE_NIGHT_YES -> mDarkThemeModePref = appInt(R.integer.value_pref_dark_theme_enabled)
                MODE_NIGHT_NO -> mDarkThemeModePref = appInt(R.integer.value_pref_dark_theme_disabled)
                MODE_NIGHT_FOLLOW_SYSTEM -> mDarkThemeModePref = appInt(R.integer.value_pref_dark_theme_follow_system)
            }
        }

    fun toggleDarkThemeMode() {
        darkThemeMode = if (darkThemeMode == MODE_NIGHT_YES) {
            MODE_NIGHT_NO
        } else {
            MODE_NIGHT_YES
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

    var defaultIme by StringOrNullPref(KEY_DEFAULT_IME)
}