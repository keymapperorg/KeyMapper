package io.github.sds100.keymapper.data.repositories

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import io.github.sds100.keymapper.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsPreferenceRepository(
    context: Context,
    private val coroutineScope: CoroutineScope,
) : PreferenceRepository {

    companion object {
        private const val DEFAULT_SHARED_PREFS_NAME = "${Constants.PACKAGE_NAME}_preferences"
    }

    private val ctx = context.applicationContext

    private val sharedPreferencesMigration = SharedPreferencesMigration(
        ctx,
        DEFAULT_SHARED_PREFS_NAME,
    )

    private val Context.dataStore by preferencesDataStore(
        name = "preferences",
        produceMigrations = { listOf(sharedPreferencesMigration) },
    )

    private val dataStore = ctx.dataStore

    override fun <T> get(key: Preferences.Key<T>): Flow<T?> = dataStore.data.map { it[key] }.distinctUntilChanged()

    override fun <T> set(key: Preferences.Key<T>, value: T?) {
        coroutineScope.launch {
            dataStore.edit {
                if (value == null) {
                    it.remove(key)
                } else {
                    it[key] = value
                }
            }
        }
    }

    override fun deleteAll() {
        coroutineScope.launch {
            dataStore.edit { it.clear() }
        }
    }
}
