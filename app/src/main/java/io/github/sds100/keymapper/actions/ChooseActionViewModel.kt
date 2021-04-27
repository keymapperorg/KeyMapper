package io.github.sds100.keymapper.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Created by sds100 on 30/03/2020.
 */

class ChooseActionViewModel : ViewModel() {

    var currentTabPosition: Int = 0

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChooseActionViewModel() as T
        }
    }
}