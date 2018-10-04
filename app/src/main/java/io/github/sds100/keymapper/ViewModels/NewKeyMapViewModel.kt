package io.github.sds100.keymapper.ViewModels

import android.app.Application
import io.github.sds100.keymapper.KeymapLiveData

/**
 * Created by sds100 on 04/10/2018.
 */

class NewKeyMapViewModel(application: Application) : ConfigKeyMapViewModel(application) {
    override val keyMap: KeymapLiveData = KeymapLiveData()

    override fun saveKeymap() {
        keyMapRepository.putKeyMap(keyMap.value!!)
    }
}