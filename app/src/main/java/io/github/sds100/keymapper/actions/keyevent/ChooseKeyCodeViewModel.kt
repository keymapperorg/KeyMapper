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
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ChooseKeyCodeViewModel : ViewModel(),
    PopupViewModel by PopupViewModelImpl() {

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

    private val _returnResult = MutableStateFlow<ActionData.InputKeyEvent?>(null)
    val returnResult: StateFlow<ActionData.InputKeyEvent?> = _returnResult.asStateFlow()

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
            _returnResult.emit(ActionData.InputKeyEvent(keyCode = id.toInt(), description = null))
        }
    }
}
