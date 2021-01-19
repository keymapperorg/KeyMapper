package io.github.sds100.keymapper.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.remove
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 19/01/21.
 */

class GlobalPreferences(private val dataStore: DataStore<Preferences>) : IGlobalPreferences {

    override fun <T> getFlow(key: Preferences.Key<T>): Flow<T?> {
        return dataStore.data.map { it[key] }
    }

    override suspend fun <T> get(key: Preferences.Key<T>): T? {
        return dataStore.data.first()[key]
    }

    override suspend fun <T> set(key: Preferences.Key<T>, value: T?) {
        dataStore.edit {
            if (value == null) {
                it.remove(key)
            } else {
                it[key] = value
            }
        }
    }
}