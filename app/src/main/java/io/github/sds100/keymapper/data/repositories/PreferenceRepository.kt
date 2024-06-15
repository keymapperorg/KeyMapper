package io.github.sds100.keymapper.data.repositories

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 13/02/21.
 */
interface PreferenceRepository {
    fun <T> get(key: Preferences.Key<T>): Flow<T?>
    fun <T> set(key: Preferences.Key<T>, value: T?)
}
