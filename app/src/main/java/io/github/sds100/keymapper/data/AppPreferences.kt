package io.github.sds100.keymapper.data

import androidx.appcompat.app.AppCompatDelegate.*
import io.github.sds100.keymapper.R
import splitties.experimental.ExperimentalSplittiesApi
import splitties.init.appCtx
import splitties.preferences.DefaultPreferences
import splitties.preferences.SuspendPrefsAccessor
import splitties.resources.int
import splitties.resources.str

/**
 * Created by sds100 on 20/02/2020.
 */

@ExperimentalSplittiesApi
class AppPreferences private constructor() : DefaultPreferences() {
    companion object : SuspendPrefsAccessor<AppPreferences>(::AppPreferences)

    private var mDarkThemeModePref by IntPref(
            appCtx.str(R.string.key_pref_dark_theme_mode),
            appCtx.int(R.integer.default_value_dark_theme_mode)
    )

    @NightMode
    var darkThemeMode: Int
        get() = when (mDarkThemeModePref) {
            appCtx.int(R.integer.value_pref_dark_theme_enabled) -> MODE_NIGHT_YES
            appCtx.int(R.integer.value_pref_dark_theme_disabled) -> MODE_NIGHT_NO
            appCtx.int(R.integer.value_pref_dark_theme_follow_system) -> MODE_NIGHT_FOLLOW_SYSTEM
            else -> MODE_NIGHT_FOLLOW_SYSTEM
        }
        set(value) {
            when (value) {
                MODE_NIGHT_YES -> mDarkThemeModePref = appCtx.int(R.integer.value_pref_dark_theme_enabled)
                MODE_NIGHT_NO -> mDarkThemeModePref = appCtx.int(R.integer.value_pref_dark_theme_disabled)
                MODE_NIGHT_FOLLOW_SYSTEM -> mDarkThemeModePref = appCtx.int(R.integer.value_pref_dark_theme_follow_system)
            }
        }

    fun toggleDarkThemeMode() {
        darkThemeMode = if (darkThemeMode == MODE_NIGHT_YES) {
            MODE_NIGHT_NO
        } else {
            MODE_NIGHT_YES
        }
    }
}