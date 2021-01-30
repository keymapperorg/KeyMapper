package io.github.sds100.keymapper.data.db

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Created by sds100 on 18/05/2020.
 */
interface IDataStoreManager {
    val fingerprintGestureDataStore: DataStore<Preferences>
    val globalPreferenceDataStore: DataStore<Preferences>
}