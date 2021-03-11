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
        getStringSetFromSharedPrefs(key, defValues)

    override fun putStringSet(key: String, defValues: MutableSet<String>?) =
        setStringSetFromSharedPrefs(key, defValues)

    private inline fun <reified T> getFromSharedPrefs(key: String, default: T): T {
        return runBlocking {
            when (default) {
                is String? -> globalPreferences.get(stringPreferencesKey(key)) ?: default
                is Boolean? -> globalPreferences.get(booleanPreferencesKey(key)) ?: default
                is Int? -> globalPreferences.get(intPreferencesKey(key)) ?: default
                is Long? -> globalPreferences.get(longPreferencesKey(key)) ?: default
                is Float? -> globalPreferences.get(floatPreferencesKey(key)) ?: default
                is Double? -> globalPreferences.get(doublePreferencesKey(key)) ?: default
                else -> {
                    val type = T::class.java.name
                    throw IllegalArgumentException("Don't know how to set a value in shared preferences for this type $type")
                }
            } as T
        }
    }

    private inline fun <reified T : Any> setFromSharedPrefs(key: String?, value: T?) {
        key ?: return

        when (value) {
            is String -> globalPreferences.set(stringPreferencesKey(key), value)
            is Boolean -> globalPreferences.set(booleanPreferencesKey(key), value)
            is Int -> globalPreferences.set(intPreferencesKey(key), value)
            is Long -> globalPreferences.set(longPreferencesKey(key), value)
            is Float -> globalPreferences.set(floatPreferencesKey(key), value)
            is Double -> globalPreferences.set(doublePreferencesKey(key), value)
            else -> {
                val type = value?.let { it::class.java.name }
                throw IllegalArgumentException("Don't know how to set a value in shared preferences for this type $type")
            }
        }
    }

    private fun getStringSetFromSharedPrefs(key: String, default: Set<String>?): Set<String> {
        return runBlocking {
            globalPreferences.get(stringSetPreferencesKey(key)) ?: emptySet()
        }
    }

    private fun setStringSetFromSharedPrefs(key: String?, value: Set<String>?) {
        key ?: return

        globalPreferences.set(stringSetPreferencesKey(key), value)
    }
}