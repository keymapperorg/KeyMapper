package io.github.sds100.keymapper.data

import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Created by sds100 on 18/05/2020.
 */
interface IPreferenceDataStore {
    fun getBoolPref(@StringRes key: Int): Boolean
    fun setBoolPref(@StringRes key: Int, value: Boolean)

    fun getStringPref(@StringRes key: Int): String?
    fun setStringPref(@StringRes key: Int, value: String)

    val fingerprintGestureDataStore: DataStore<Preferences>
}