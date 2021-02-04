package io.github.sds100.keymapper.util

import androidx.datastore.preferences.core.*
import androidx.preference.PreferenceDataStore
import io.github.sds100.keymapper.data.IGlobalPreferences
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 19/01/21.
 */
class SharedPrefsDataStoreWrapper(
    private val globalPreferences: IGlobalPreferences
) : PreferenceDataStore() {

    override fun getBoolean(key: String, defValue: Boolean) = getFromSharedPrefs(key, defValue)
    override fun putBoolean(key: String, value: Boolean) = setFromSharedPrefs(key, value)

    override fun getString(key: String, defValue: String?) = getFromSharedPrefs(key, defValue)
    override fun putString(key: String, value: String?) = setFromSharedPrefs(key, value)

    override fun getInt(key: String, defValue: Int) = getFromSharedPrefs(key, defValue)
    override fun putInt(key: String, value: Int) = setFromSharedPrefs(key, value)

    override fun getStringSet(key: String, defValues: MutableSet<String>?) =
        getSetFromSharedPrefs(key, defValues)

    override fun putStringSet(key: String, defValues: MutableSet<String>?) =
        setSetFromSharedPrefs(key, defValues)

    private inline fun <reified T> getFromSharedPrefs(key: String, default: T): T {
        return runBlocking {
            globalPreferences.get(
                    when (default) {
                        is Int -> intPreferencesKey(key)
                        is Long -> longPreferencesKey(key)
                        is String -> stringPreferencesKey(key)
                        is Boolean -> booleanPreferencesKey(key)
                        is Double -> doublePreferencesKey(key)
                        is Float -> floatPreferencesKey(key)
                        else -> throw IllegalArgumentException("Invalid type ${T::class.java}")
                    }
            ) as T? ?: default
        }
    }

    private inline fun <reified T : Any> setFromSharedPrefs(key: String?, value: T?) {
        key ?: return

        globalPreferences.set(
                when (value) {
                    is Int -> intPreferencesKey(key)
                    is Long -> longPreferencesKey(key)
                    is String -> stringPreferencesKey(key)
                    is Boolean -> booleanPreferencesKey(key)
                    is Double -> doublePreferencesKey(key)
                    is Float -> floatPreferencesKey(key)
                    else -> throw IllegalArgumentException("Invalid type ${T::class.java}")
                } as Preferences.Key<T>, value)
    }

    private inline fun <reified T : Any> getSetFromSharedPrefs(key: String, default: Set<T>?): Set<T> {
        return runBlocking {
            globalPreferences.get(stringSetPreferencesKey(key)) as Set<T>? ?: emptySet()
        }
    }

    private inline fun <reified T : Any> setSetFromSharedPrefs(key: String?, value: Set<T>?) {
        key ?: return

        globalPreferences.set(stringSetPreferencesKey(key) as Preferences.Key<Set<T>>, value)
    }
}