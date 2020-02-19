package com.example.architecturetest.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.architecturetest.data.KeymapRepository
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Trigger
import kotlinx.coroutines.launch

open class ConfigKeymapViewModel internal constructor(
        private val repository: KeymapRepository,
        val id: Long? = null
) : ViewModel() {

    val trigger: MutableLiveData<Trigger> = MutableLiveData()
    val actionList: MutableLiveData<List<Action>> = MutableLiveData()
    val flags: MutableLiveData<Int> = MutableLiveData()
    val isEnabled: MutableLiveData<Boolean> = MutableLiveData()

    init {
        if (id == null) {
            trigger.value = null
            actionList.value = listOf()
            flags.value = 0
            isEnabled.value = false

        } else {
            viewModelScope.launch {
                repository.getKeymap(id).let { keymap ->
                    actionList.value = keymap.actionList
                }
            }
        }
    }

    class Factory(
            private val mRepository: KeymapRepository, private val mId: Long
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
                ConfigKeymapViewModel(mRepository, mId) as T
    }
}