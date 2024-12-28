package io.github.sds100.keymapper.sorting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SortViewModel(
    private val sortKeyMapsUseCase: SortKeyMapsUseCase,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    val showHelp = preferenceRepository.get(Keys.sortShowHelp)
        .map { it ?: true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    val state: MutableStateFlow<List<SortFieldOrder>>

    init {
        val list = runBlocking { sortKeyMapsUseCase.observeSortFieldOrder().first() }

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

            it.mapIndexed { i, sortFieldOrder ->
                if (i != index) {
                    return@mapIndexed sortFieldOrder
                }

                val newOrder = sortFieldOrder.order.toggle()
                sortFieldOrder.copy(order = newOrder)
            }
        }
    }

    fun resetSortPriority() {
        state.update {
            it.map { sortFieldOrder ->
                sortFieldOrder.copy(order = SortOrder.NONE)
            }
        }
    }

    fun applySortPriority() {
        viewModelScope.launch {
            sortKeyMapsUseCase.setSortFieldOrder(state.value)
            saveState()
        }
    }

    fun setShowHelp(show: Boolean) {
        viewModelScope.launch {
            preferenceRepository.set(Keys.sortShowHelp, show)
        }
    }

    fun showExample() {
        viewModelScope.launch {
            state.value = listOf(
                SortFieldOrder(SortField.ACTIONS, SortOrder.ASCENDING),
                SortFieldOrder(SortField.TRIGGER, SortOrder.DESCENDING),
                SortFieldOrder(SortField.CONSTRAINTS),
                SortFieldOrder(SortField.OPTIONS),
            )
        }
    }

    class Factory(
        private val sortKeyMapsUseCase: SortKeyMapsUseCase,
        private val preferenceRepository: PreferenceRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            SortViewModel(
                sortKeyMapsUseCase,
                preferenceRepository,
            ) as T
    }

    private var savedState: List<SortFieldOrder>? = null

    private fun saveState(toSave: List<SortFieldOrder> = state.value) {
        savedState = toSave.toList()
    }

    fun restoreState() {
        savedState?.let {
            state.value = it
        }
    }
}
