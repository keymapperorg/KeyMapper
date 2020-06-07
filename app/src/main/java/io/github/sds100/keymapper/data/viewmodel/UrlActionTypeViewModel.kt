package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Created by sds100 on 31/03/2020.
 */
class UrlActionTypeViewModel : ViewModel() {

    val url = MutableLiveData("")

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return UrlActionTypeViewModel() as T
        }
    }
}