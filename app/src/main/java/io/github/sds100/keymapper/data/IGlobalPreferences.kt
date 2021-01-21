package io.github.sds100.keymapper.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 19/01/21.
 */
interface IGlobalPreferences {
    fun <T> getFlow(key: Preferences.Key<T>): Flow<T?>
    suspend fun <T> get(key: Preferences.Key<T>): T?
    suspend fun <T> set(key: Preferences.Key<T>, value: T?)
}

fun IGlobalPreferences.darkThemeMode() = getFlow(PreferenceKeys.darkTheme).map {
    when (it) {
        "0" -> AppCompatDelegate.MODE_NIGHT_YES
        "1" -> AppCompatDelegate.MODE_NIGHT_NO
        "2" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}

fun IGlobalPreferences.appIntro() = getFlow(PreferenceKeys.appIntro).map {
    it ?: false
}