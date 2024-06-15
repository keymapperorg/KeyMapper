package io.github.sds100.keymapper.util

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.preference.PreferenceDataStore
import io.github.sds100.keymapper.settings.ConfigSettingsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 19/01/21.
 */
class SharedPrefsDataStoreWrapper(
    private val configSettingsUseCase: ConfigSettingsUseCase,
) : PreferenceDataStore() {

    override fun getBoolean(key: String, defValue: Boolean) = getFromSharedPrefs(key, defValue)
    override fun putBoolean(key: String, value: Boolean) = setFromSharedPrefs(key, value)

    override fun getString(key: String, defValue: String?) = getFromSharedPrefs(key, defValue ?: "")
    override fun putString(key: String, value: String?) = setFromSharedPrefs(key, value)

    override fun getInt(key: String, defValue: Int) = getFromSharedPrefs(key, defValue)
    override fun putInt(key: String, value: Int) = setFromSharedPrefs(key, value)

    override fun getStringSet(key: String, defValues: MutableSet<String>?) =
        getStringSetFromSharedPrefs(key, defValues ?: emptySet())

    override fun putStringSet(key: String, defValues: MutableSet<String>?) =
        setStringSetFromSharedPrefs(key, defValues)

    private inline fun <reified T> getFromSharedPrefs(key: String, default: T): T = runBlocking {
        when (default) {
            is String? -> configSettingsUseCase.getPreference(stringPreferencesKey(key)).first()
                ?: default

            is Boolean? -> configSettingsUseCase.getPreference(booleanPreferencesKey(key))
                .first() ?: default

            is Int? -> configSettingsUseCase.getPreference(intPreferencesKey(key)).first()
                ?: default

            is Long? -> configSettingsUseCase.getPreference(longPreferencesKey(key)).first()
                ?: default

            is Float? -> configSettingsUseCase.getPreference(floatPreferencesKey(key)).first()
                ?: default

            is Double? -> configSettingsUseCase.getPreference(doublePreferencesKey(key)).first()
                ?: default

            else -> {
                val type = T::class.java.name
                throw IllegalArgumentException("Don't know how to set a value in shared preferences for this type $type")
            }
        } as T
    }

    private inline fun <reified T : Any> setFromSharedPrefs(key: String?, value: T?) {
        key ?: return

        when (value) {
            is String -> configSettingsUseCase.setPreference(stringPreferencesKey(key), value)
            is Boolean -> configSettingsUseCase.setPreference(booleanPreferencesKey(key), value)
            is Int -> configSettingsUseCase.setPreference(intPreferencesKey(key), value)
            is Long -> configSettingsUseCase.setPreference(longPreferencesKey(key), value)
            is Float -> configSettingsUseCase.setPreference(floatPreferencesKey(key), value)
            is Double -> configSettingsUseCase.setPreference(doublePreferencesKey(key), value)
            else -> {
                val type = value?.let { it::class.java.name }
                throw IllegalArgumentException("Don't know how to set a value in shared preferences for this type $type")
            }
        }
    }

    private fun getStringSetFromSharedPrefs(key: String, default: Set<String>?): Set<String> =
        runBlocking {
            configSettingsUseCase.getPreference(stringSetPreferencesKey(key)).first() ?: emptySet()
        }

    private fun setStringSetFromSharedPrefs(key: String?, value: Set<String>?) {
        key ?: return

        configSettingsUseCase.setPreference(stringSetPreferencesKey(key), value)
    }
}
