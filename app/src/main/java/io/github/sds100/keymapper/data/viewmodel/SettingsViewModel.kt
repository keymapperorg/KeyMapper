package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.data.IGlobalPreferences
import io.github.sds100.keymapper.util.SharedPrefsDataStoreWrapper

/**
 * Created by sds100 on 19/01/21.
 */
class SettingsViewModel(globalPreferences: IGlobalPreferences) : ViewModel() {
    val sharedPrefsDataStoreWrapper = SharedPrefsDataStoreWrapper(globalPreferences)

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val globalPreferences: IGlobalPreferences
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SettingsViewModel(globalPreferences) as T
        }
    }
}