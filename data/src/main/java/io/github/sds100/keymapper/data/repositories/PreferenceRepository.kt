package io.github.sds100.keymapper.data.repositories

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow


interface PreferenceRepository {
    fun <T> get(key: Preferences.Key<T>): Flow<T?>
    fun <T> set(key: Preferences.Key<T>, value: T?)
    fun <T> update(key: Preferences.Key<T>, update: suspend (T?) -> T?)
    fun deleteAll()
}
