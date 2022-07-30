package io.github.sds100.keymapper.actions.keyevent

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.util.containsQuery
import io.github.sds100.keymapper.util.ui.SearchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Created by sds100 on 30/07/2022.
 */

@HiltViewModel
class ChooseKeyCodeViewModel2 @Inject constructor() : ViewModel() {

    private val _searchState: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val allListItems: List<KeyCodeListItem> = createAllListItems()
    var listItems: List<KeyCodeListItem> by mutableStateOf(allListItems)
        private set

    init {
        viewModelScope.launch(Dispatchers.Default) {
            searchState.collectLatest { searchState ->
                val newListItems = when (searchState) {
                    SearchState.Idle -> allListItems
                    is SearchState.Searching -> {
                        filterListItems(searchState.query)
                    }
                }

                withContext(Dispatchers.Main) {
                    listItems = newListItems
                }
            }
        }
    }

    fun setSearchState(state: SearchState) {
        _searchState.value = state
    }

    private fun filterListItems(query: String): List<KeyCodeListItem> {
        return allListItems.filter { it.label.containsQuery(query) }
    }

    private fun createAllListItems(): List<KeyCodeListItem> {
        return KeyEventUtils.getKeyCodes()
            .sorted()
            .map { keyCode ->
                KeyCodeListItem(
                    keyCode = keyCode,
                    label = "$keyCode \t\t ${KeyEvent.keyCodeToString(keyCode)}"
                )
            }
    }
}

data class KeyCodeListItem(val keyCode: Int, val label: String)