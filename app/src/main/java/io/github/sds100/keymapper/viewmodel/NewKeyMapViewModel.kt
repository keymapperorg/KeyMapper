package io.github.sds100.keymapper.viewmodel

import android.app.Application
import io.github.sds100.keymapper.KeymapLiveData

/**
 * Created by sds100 on 04/10/2018.
 */

class NewKeyMapViewModel(application: Application) : ConfigKeyMapViewModel(application) {
    override val keyMap: KeymapLiveData = KeymapLiveData()

    init {
        keyMap.notifyObservers()
    }

    override fun saveKeymap() {
        keyMapRepository.insertKeyMap(keyMap.value!!)
    }
}