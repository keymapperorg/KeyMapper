package io.github.sds100.keymapper.data.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.util.KeyEventUtils

/**
 * Created by sds100 on 30/03/2020.
 */

class KeyActionTypeViewModel : ViewModel() {
    val keyEvent = MutableLiveData<KeyEvent>()

    val keyLabel = Transformations.map(keyEvent) {
        it ?: return@map null

        KeyEventUtils.keycodeToString(it.keyCode)
    }

    fun clearKey() {
        keyEvent.value = null
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return KeyActionTypeViewModel() as T
        }
    }
}