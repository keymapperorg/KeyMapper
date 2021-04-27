package io.github.sds100.keymapper.system.phone

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ChoosePhoneNumberViewModel : ViewModel() {

    val number = MutableLiveData("")

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChoosePhoneNumberViewModel() as T
        }
    }
}