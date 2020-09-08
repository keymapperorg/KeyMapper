package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.util.Event

/**
 * Created by sds100 on 08/09/20.
 */
class CreateActionShortcutViewModel : ViewModel() {

    val actionList = MutableLiveData(listOf<Action>())

    val addActionEvent = MutableLiveData<Event<Unit>>()

    fun addAction() {
        addActionEvent.value = Event(Unit)
    }

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = CreateActionShortcutViewModel() as T
    }
}