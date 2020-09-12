package io.github.sds100.keymapper.data

import androidx.annotation.StringRes

/**
 * Created by sds100 on 18/05/2020.
 */
interface IPreferenceDataStore {
    fun getBoolPref(@StringRes key: Int): Boolean
    fun setBoolPref(@StringRes key: Int, value: Boolean)
}