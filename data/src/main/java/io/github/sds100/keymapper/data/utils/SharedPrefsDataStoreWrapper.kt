package io.github.sds100.keymapper.data.utils

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.preference.PreferenceDataStore
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class SharedPrefsDataStoreWrapper @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
) : PreferenceDataStore() {

    override fun getBoolean(key: String, defValue: Boolean) = getFromSharedPrefs(key, defValue)
    override fun putBoolean(key: String, value: Boolean) = setFromSharedPrefs(key, value)

    override fun getString(key: String, defValue: String?) = getFromSharedPrefs(key, defValue ?: "")
    override fun putString(key: String, value: String?) = setFromSharedPrefs(key, value)

    override fun getInt(key: String, defValue: Int) = getFromSharedPrefs(key, defValue)
    override fun putInt(key: String, value: Int) = setFromSharedPrefs(key, value)

    override fun getStringSet(key: String, defValues: MutableSet<String>?) = getStringSetFromSharedPrefs(key, defValues ?: emptySet())

    override fun putStringSet(key: String, defValues: MutableSet<String>?) = setStringSetFromSharedPrefs(key, defValues)

    private inline fun <reified T> getFromSharedPrefs(key: String, default: T): T = runBlocking {
        when (default) {
            is String? -> preferenceRepository.get(stringPreferencesKey(key)).first()
                ?: default

            is Boolean? -> preferenceRepository.get(booleanPreferencesKey(key))
                .first() ?: default

            is Int? -> preferenceRepository.get(intPreferencesKey(key)).first()
                ?: default

            is Long? -> preferenceRepository.get(longPreferencesKey(key)).first()
                ?: default

            is Float? -> preferenceRepository.get(floatPreferencesKey(key)).first()
                ?: default

            is Double? -> preferenceRepository.get(doublePreferencesKey(key)).first()
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
            is String -> preferenceRepository.set(stringPreferencesKey(key), value)
            is Boolean -> preferenceRepository.set(booleanPreferencesKey(key), value)
            is Int -> preferenceRepository.set(intPreferencesKey(key), value)
            is Long -> preferenceRepository.set(longPreferencesKey(key), value)
            is Float -> preferenceRepository.set(floatPreferencesKey(key), value)
            is Double -> preferenceRepository.set(doublePreferencesKey(key), value)
            else -> {
                val type = value?.let { it::class.java.name }
                throw IllegalArgumentException("Don't know how to set a value in shared preferences for this type $type")
            }
        }
    }

    private fun getStringSetFromSharedPrefs(key: String, default: Set<String>?): Set<String> = runBlocking {
        preferenceRepository.get(stringSetPreferencesKey(key)).first() ?: emptySet()
    }

    private fun setStringSetFromSharedPrefs(key: String?, value: Set<String>?) {
        key ?: return

        preferenceRepository.set(stringSetPreferencesKey(key), value)
    }
}
