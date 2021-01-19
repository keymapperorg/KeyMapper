package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.IDataStoreManager
import io.github.sds100.keymapper.util.SharedPrefsDataStoreWrapper

/**
 * Created by sds100 on 19/01/21.
 */
class SettingsViewModel(private val dataStoreManager: IDataStoreManager) : ViewModel() {
    val sharedPrefsDataStoreWrapper = SharedPrefsDataStoreWrapper(
        viewModelScope,
        dataStoreManager
    )

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val dataStoreManager: IDataStoreManager
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SettingsViewModel(dataStoreManager) as T
        }
    }
}