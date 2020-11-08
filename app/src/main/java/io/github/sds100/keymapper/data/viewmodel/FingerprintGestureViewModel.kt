package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.data.IPreferenceDataStore

class FingerprintGestureViewModel(private val mPreferenceDataStore: IPreferenceDataStore) : ViewModel() {

    @Suppress("UNCHECKED_CAST")
    class Factory(val preferenceDataStore: IPreferenceDataStore) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FingerprintGestureViewModel(preferenceDataStore) as T
        }
    }
}