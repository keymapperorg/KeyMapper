package io.github.sds100.keymapper.sorting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SortViewModel(
    private val setKeyMapFieldSortOrderUseCase: SetKeyMapFieldSortOrderUseCase,
    private val observeKeyMapFieldSortOrderUseCase: ObserveKeyMapFieldSortOrderUseCase,
    private val setSortFieldPriorityUseCase: SetSortFieldPriorityUseCase,
    private val observeSortFieldPriorityUseCase: ObserveSortFieldPriorityUseCase,
) : ViewModel() {
    val state: MutableStateFlow<List<SortFieldOrder>>

    init {
        val list = runBlocking {
            observeSortFieldPriorityUseCase().map {
                it.map { field ->
                    val order = runBlocking { observeKeyMapFieldSortOrderUseCase(field).first() }

                    SortFieldOrder(field, order)
                }
            }.first()
        }

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
            val index = it.indexOfFirst { it.sortField == field }
            val newOrder = it[index].sortOrder.toggle()
            val newList = it.toMutableList()
            newList[index] = SortFieldOrder(field, newOrder)
            newList
        }
    }

    fun applySortPriority() {
        viewModelScope.launch {
            setSortFieldPriorityUseCase(state.value.map { it.sortField }.toSet())
            state.value.forEach {
                setKeyMapFieldSortOrderUseCase(it.sortField, it.sortOrder)
            }
        }
    }

    class Factory(
        private val setKeyMapFieldSortOrderUseCase: SetKeyMapFieldSortOrderUseCase,
        private val observeKeyMapFieldSortOrderUseCase: ObserveKeyMapFieldSortOrderUseCase,
        private val setSortFieldPriorityUseCase: SetSortFieldPriorityUseCase,
        private val observeSortFieldPriorityUseCase: ObserveSortFieldPriorityUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            SortViewModel(
                setKeyMapFieldSortOrderUseCase,
                observeKeyMapFieldSortOrderUseCase,
                setSortFieldPriorityUseCase,
                observeSortFieldPriorityUseCase,
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

data class SortFieldOrder(
    val sortField: SortField,
    val sortOrder: SortOrder,
)
