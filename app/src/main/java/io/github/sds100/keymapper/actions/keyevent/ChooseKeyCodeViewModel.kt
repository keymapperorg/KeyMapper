package io.github.sds100.keymapper.actions.keyevent

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.common.state.State
import io.github.sds100.keymapper.util.filterByQuery
import io.github.sds100.keymapper.util.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.util.ui.SimpleListItemOld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChooseKeyCodeViewModel : ViewModel() {

    val searchQuery = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow<State<List<SimpleListItemOld>>>(State.Loading)
    val state = _state.asStateFlow()

    private val allListItems = flow {
        withContext(Dispatchers.Default) {
            InputEventUtils.getKeyCodes().sorted().map { keyCode ->
                DefaultSimpleListItem(
                    id = keyCode.toString(),
                    title = "$keyCode \t\t ${KeyEvent.keyCodeToString(keyCode)}",
                )
            }
        }.let { emit(it) }
    }

    private val _returnResult = MutableSharedFlow<Int>()
    val returnResult = _returnResult.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                searchQuery,
                allListItems,
            ) { query, allListItems ->
                Pair(allListItems, query)
            }.collectLatest { pair ->
                val (allListItems, query) = pair

                allListItems.filterByQuery(query).collect {
                    _state.value = it
                }
            }
        }
    }

    fun onListItemClick(id: String) {
        viewModelScope.launch {
            _returnResult.emit(id.toInt())
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChooseKeyCodeViewModel() as T
    }
}
