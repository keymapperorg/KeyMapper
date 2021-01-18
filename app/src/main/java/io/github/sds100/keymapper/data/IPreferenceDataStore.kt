package io.github.sds100.keymapper.data

import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 18/05/2020.
 */
interface IPreferenceDataStore {
    fun getBoolPref(@StringRes key: Int): Boolean
    fun setBoolPref(@StringRes key: Int, value: Boolean)

    fun <T> getFlow(key: Preferences.Key<T>): Flow<T?>
    suspend fun <T> get(key: Preferences.Key<T>): T?
    suspend fun <T> set(key: Preferences.Key<T>, value: T)

    fun getStringPref(@StringRes key: Int): String?
    fun setStringPref(@StringRes key: Int, value: String)

    val fingerprintGestureDataStore: DataStore<Preferences>
}