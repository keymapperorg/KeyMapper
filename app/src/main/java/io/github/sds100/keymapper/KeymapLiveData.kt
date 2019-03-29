package io.github.sds100.keymapper

import androidx.lifecycle.MutableLiveData

/**
 * Created by sds100 on 04/10/2018.
 */

class KeymapLiveData(keyMap: KeyMap = KeyMap(id = 0)) : MutableLiveData<KeyMap>() {

    var action: Action?
        get() = value?.action
        set(newAction) {
            this.value?.action = newAction
            notifyObservers()
        }

    init {
        value = keyMap
    }

    fun addTrigger(vararg trigger: Trigger) {
        value?.apply { triggerList.addAll(trigger) }
        notifyObservers()
    }

    fun notifyObservers() {
        this.postValue(this.value)
    }
}