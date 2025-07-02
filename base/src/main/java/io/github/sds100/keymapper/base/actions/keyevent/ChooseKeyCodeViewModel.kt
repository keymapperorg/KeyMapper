package io.github.sds100.keymapper.base.actions.keyevent

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.filterByQuery
import io.github.sds100.keymapper.base.utils.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProviderImpl
import io.github.sds100.keymapper.base.utils.ui.SimpleListItemOld
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
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
import javax.inject.Inject

@HiltViewModel
class ChooseKeyCodeViewModel @Inject constructor() : ViewModel() {

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
}
