package io.github.sds100.keymapper.viewmodel

import android.app.Application
import io.github.sds100.keymapper.KeymapLiveData
import org.jetbrains.anko.doAsync

/**
 * Created by sds100 on 04/10/2018.
 */

class NewKeyMapViewModel(application: Application) : ConfigKeyMapViewModel(application) {
    //create a blank keymap
    override val keyMap = KeymapLiveData()

    init {
        keyMap.notifyObservers()
    }

    override fun saveKeymap() {
        keyMap.value?.let {
            doAsync { db.keyMapDao().insert(it) }
        }
    }
}