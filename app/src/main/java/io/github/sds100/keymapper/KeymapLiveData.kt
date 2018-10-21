package io.github.sds100.keymapper

import androidx.lifecycle.LiveData

/**
 * Created by sds100 on 04/10/2018.
 */

/**
 * @param keyMap The initial [KeyMap] object to use
 */
class KeymapLiveData(keyMap: KeyMap = KeyMap(0L)) : LiveData<KeyMap>() {

    var action: Action?
        get() = value!!.action
        set(newAction) {
            this.value!!.action = newAction
            notifyObservers()
        }

    init {
        value = keyMap
    }

    fun addTrigger(vararg trigger: Trigger) {
        value!!.triggerList.addAll(trigger)
        notifyObservers()
    }

    fun notifyObservers() {
        this.postValue(this.value)
    }
}