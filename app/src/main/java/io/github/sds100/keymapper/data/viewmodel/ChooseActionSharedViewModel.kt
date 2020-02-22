package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.sds100.keymapper.data.model.Action

/**
 * Created by sds100 on 27/01/2020.
 */

class ChooseActionSharedViewModel : ViewModel() {
    val chosenAction = MutableLiveData<Action>()

    fun selectAction(action: Action) {
        chosenAction.value = action
    }
}