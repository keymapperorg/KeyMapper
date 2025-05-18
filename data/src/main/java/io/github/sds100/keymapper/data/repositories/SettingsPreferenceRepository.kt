package io.github.sds100.keymapper.data.repositories

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsPreferenceRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val coroutineScope: CoroutineScope,
) : PreferenceRepository {

    private val ctx = context.applicationContext

    private val Context.dataStore by preferencesDataStore(name = "preferences")

    private val dataStore = ctx.dataStore

    override fun <T> get(key: Preferences.Key<T>): Flow<T?> = dataStore.data.map { it[key] }.distinctUntilChanged()

    override fun <T> set(key: Preferences.Key<T>, value: T?) {
        coroutineScope.launch {
            dataStore.updateData {
                val prefs = it.toMutablePreferences()

                if (value == null) {
                    prefs.remove(key)
                } else {
                    prefs[key] = value
                }

                prefs
            }
        }
    }

    override fun deleteAll() {
        coroutineScope.launch {
            dataStore.edit { it.clear() }
        }
    }

    override fun <T> update(key: Preferences.Key<T>, update: suspend (T?) -> T?) {
        coroutineScope.launch {
            dataStore.updateData {
                val prefs = it.toMutablePreferences()

                val newValue = update(prefs[key])

                if (newValue == null) {
                    prefs.remove(key)
                } else {
                    prefs[key] = newValue
                }

                prefs
            }
        }
    }
}
