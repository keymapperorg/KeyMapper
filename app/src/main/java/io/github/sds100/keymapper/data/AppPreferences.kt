package io.github.sds100.keymapper.data

import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import io.github.sds100.keymapper.R
import splitties.experimental.ExperimentalSplittiesApi
import splitties.init.appCtx
import splitties.preferences.DefaultPreferences
import splitties.preferences.SuspendPrefsAccessor
import splitties.resources.appBool
import splitties.resources.appInt
import splitties.resources.appStr

/**
 * Created by sds100 on 20/02/2020.
 */

@ExperimentalSplittiesApi
class AppPreferences private constructor() : DefaultPreferences() {
    companion object : SuspendPrefsAccessor<AppPreferences>(::AppPreferences) {
        val hasRootPermission: Boolean
            get() = PreferenceManager.getDefaultSharedPreferences(appCtx).getBoolean(
                appStr(R.string.key_pref_root_permission),
                appBool(R.bool.default_value_root_permission)
            )
    }

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
}