package io.github.sds100.keymapper.ViewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.Data.KeyMapRepository
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.Trigger

/**
 * Created by sds100 on 05/09/2018.
 */

class NewKeyMapViewModel(application: Application) : AndroidViewModel(application) {
    val chosenAction: MutableLiveData<Action> = MutableLiveData()
    val triggerList: MutableLiveData<MutableList<Trigger>> = MutableLiveData()

    init {
        triggerList.value = mutableListOf()
    }

    private val mKeyMapRepository = KeyMapRepository.getInstance(application.applicationContext)

    fun saveKeyMap(keyMap: KeyMap) {
        mKeyMapRepository.addKeyMap(keyMap)
    }
}