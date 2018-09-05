package io.github.sds100.keymapper.ViewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.sds100.keymapper.Data.KeyMapRepository
import io.github.sds100.keymapper.KeyMap

/**
 * Created by sds100 on 05/09/2018.
 */

class NewKeyMapViewModel(application: Application) : AndroidViewModel(application) {
    private val mKeyMapRepository = KeyMapRepository.getInstance(application.applicationContext)

    fun saveKeyMap(keyMap: KeyMap) {
        mKeyMapRepository.addKeyMap(keyMap)
    }
}