package io.github.sds100.keymapper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Created by sds100 on 23/07/2021.
 */
class ActivityViewModel : ViewModel() {
    var previousNightMode: Int? = null

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ActivityViewModel() as T
        }
    }
}