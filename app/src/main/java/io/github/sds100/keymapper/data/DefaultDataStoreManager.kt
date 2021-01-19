package io.github.sds100.keymapper.data

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.remove
import androidx.datastore.preferences.createDataStore
import io.github.sds100.keymapper.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 20/02/2020.
 */

class DefaultDataStoreManager(context: Context) : IDataStoreManager {

    companion object {
        private const val DEFAULT_SHARED_PREFS_NAME = "${Constants.PACKAGE_NAME}_preferences"
    }

    private val ctx = context.applicationContext

    private val sharedPreferencesMigration = SharedPreferencesMigration(
        ctx,
        DEFAULT_SHARED_PREFS_NAME
    )

    override val fingerprintGestureDataStore = ctx.createDataStore("fingerprint_gestures")
    override val globalPreferenceDataStore = ctx.createDataStore(
        name = "preferences",
        migrations = listOf(sharedPreferencesMigration))

    override fun getBoolPref(key: Int): Boolean {
        TODO()
    }

    override fun setBoolPref(key: Int, value: Boolean) {
        TODO()
    }

    override fun <T> getFlow(key: Preferences.Key<T>): Flow<T?> {
        return globalPreferenceDataStore.data.map { it[key] }
    }

    override suspend fun <T> get(key: Preferences.Key<T>): T? {
        return globalPreferenceDataStore.data.first()[key]
    }

    override suspend fun <T> set(key: Preferences.Key<T>, value: T?) {
        globalPreferenceDataStore.edit {
            if (value == null) {
                it.remove(key)
            } else {
                it[key] = value
            }
        }
    }
}