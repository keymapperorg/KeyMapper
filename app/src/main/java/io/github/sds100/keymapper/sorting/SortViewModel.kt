package io.github.sds100.keymapper.sorting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SortViewModel(
    private val observeSortFieldOrderUseCase: ObserveSortFieldOrderUseCase,
    private val setKeyMapSortFieldOrderUseCase: SetKeyMapSortFieldOrderUseCase,
) : ViewModel() {
    val state: MutableStateFlow<List<SortFieldOrder>>

    init {
        val list = runBlocking { observeSortFieldOrderUseCase().first() }

        state = MutableStateFlow(list)
        saveState()
    }

    fun swapSortPriority(fromIndex: Int, toIndex: Int) {
        state.update {
            val newList = it.toMutableList()
            newList.add(toIndex, newList.removeAt(fromIndex))
            newList
        }
    }

    fun toggleSortOrder(field: SortField) {
        state.update {
            val index = it.indexOfFirst { it.field == field }
            val newOrder = it[index].order.toggle()
            val newList = it.toMutableList()
            newList[index] = SortFieldOrder(field, newOrder)
            newList
        }
    }

    fun applySortPriority() {
        viewModelScope.launch {
            setKeyMapSortFieldOrderUseCase(state.value)
        }
    }

    class Factory(
        private val observeSortFieldOrderUseCase: ObserveSortFieldOrderUseCase,
        private val setKeyMapSortFieldOrderUseCase: SetKeyMapSortFieldOrderUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            SortViewModel(
                observeSortFieldOrderUseCase,
                setKeyMapSortFieldOrderUseCase,
            ) as T
    }

    private var savedState: List<SortFieldOrder>? = null

    private fun saveState() {
        savedState = state.value.toList()
    }

    fun restoreState() {
        savedState?.let {
            state.update { it }
        }
    }
}
