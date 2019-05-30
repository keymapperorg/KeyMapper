package io.github.sds100.keymapper.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.KeymapLiveData
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by sds100 on 04/10/2018.
 */

class EditKeyMapViewModel(id: Long, application: Application) : ConfigKeyMapViewModel(application) {

    override val keyMap: KeymapLiveData = KeymapLiveData()

    init {
        doAsync {
            val newKeyMap = db.keyMapDao().getById(id)

            //livedata values can only be set on main thread
            uiThread {
                keyMap.value = newKeyMap
                keyMap.notifyObservers()
            }
        }
    }

    override fun saveKeymap() {
        keyMap.value?.let {
            doAsync { db.keyMapDao().update(it) }
        }

        keyMap.notifyObservers()
    }

    class Factory(private val mId: Long,
                  private val mApplication: Application
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = EditKeyMapViewModel(mId, mApplication) as T
    }
}