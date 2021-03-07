package io.github.sds100.keymapper.data.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.*
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Created by sds100 on 31/03/2020.
 */

class KeycodeListViewModel : ViewModel() {

    private val mKeycodeLabelMap = liveData {
        val keycodeList = withContext(viewModelScope.coroutineContext + Dispatchers.Default) {
            sequence {
                KeyEventUtils.getKeyCodes().forEach {
                    yield(it to "$it \t\t ${KeyEvent.keyCodeToString(it)}")
                }
            }.sortedBy { it.first }.toMap()
        }

        emit(keycodeList)
    }

    val searchQuery: MutableLiveData<String> = MutableLiveData("")

    val filteredKeycodeLabelList = MediatorLiveData<State<Map<Int, String>>>().apply {
        fun filter(query: String) {
            value = Loading()

            value = mKeycodeLabelMap.value?.filter {
                it.value.toLowerCase(Locale.getDefault()).contains(query)
            }.getState()
        }

        addSource(searchQuery) { query ->
            filter(query)
        }

        addSource(mKeycodeLabelMap) {
            value = Data(it)

            searchQuery.value?.let { query ->
                filter(query)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return KeycodeListViewModel() as T
        }
    }
}