package io.github.sds100.keymapper.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

/**
 * Created by sds100 on 15/01/21.
 */

inline fun <X, Y> LiveData<DataState<X>>.stateMap(crossinline transform: (X) -> DataState<Y>
): LiveData<DataState<Y>> {
    return MediatorLiveData<DataState<Y>>().apply {
        addSource(this@stateMap) { source ->
            value = Loading()

            value = source.switchMapData {
                transform.invoke(it)
            }
        }
    }
}