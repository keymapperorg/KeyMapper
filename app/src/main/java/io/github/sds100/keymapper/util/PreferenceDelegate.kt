package io.github.sds100.keymapper.util

import androidx.datastore.preferences.core.Preferences
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KProperty

/**
 * Created by sds100 on 14/02/21.
 */

class FlowPrefDelegate<T>(
    private val key: Preferences.Key<T>,
    private val defaultValue: T,
) {

    operator fun getValue(thisRef: PreferenceRepository, prop: KProperty<*>?): Flow<T> =
        thisRef.get(key).map { it ?: defaultValue }
}

class PrefDelegate<T>(
    private val key: Preferences.Key<T>,
    private val defaultValue: T,
) {

    operator fun getValue(thisRef: PreferenceRepository, prop: KProperty<*>?): T =
        thisRef.get(key).map { it ?: defaultValue }.firstBlocking()

    operator fun setValue(thisRef: PreferenceRepository, prop: KProperty<*>?, value: T?) {
        thisRef.set(key, value)
    }
}
