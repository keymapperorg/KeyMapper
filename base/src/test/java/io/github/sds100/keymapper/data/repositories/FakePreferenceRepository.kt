package io.github.sds100.keymapper.data.repositories

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePreferenceRepository : PreferenceRepository {
    private val preferences: MutableStateFlow<Map<Preferences.Key<*>, Any?>> =
        MutableStateFlow(emptyMap())

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Preferences.Key<T>): Flow<T?> {
        return preferences.map { it[key] as T? }
    }

    override fun <T> set(key: Preferences.Key<T>, value: T?) {
        preferences.value = preferences.value.toMutableMap().apply {
            this[key] = value
        }
    }

    override fun deleteAll() {
        preferences.value = emptyMap()
    }

    override fun <T> update(key: Preferences.Key<T>, update: suspend (T?) -> T?) {
        throw NotImplementedError()
    }
}
