package io.github.sds100.keymapper.data

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 19/01/21.
 */
interface IGlobalPreferences {
    fun <T> getFlow(key: Preferences.Key<T>): Flow<T?>
    suspend fun <T> get(key: Preferences.Key<T>): T?
    fun <T> set(key: Preferences.Key<T>, value: T?)
    fun toggle(key: Preferences.Key<Boolean>)
}