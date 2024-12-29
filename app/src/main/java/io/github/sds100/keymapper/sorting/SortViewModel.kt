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
            initialValue = runBlocking {
                preferenceRepository.get(Keys.sortShowHelp).first()
            } ?: true,
        )

    val sortFieldOrder: MutableStateFlow<List<SortFieldOrder>> = MutableStateFlow(emptyList())

    init {
        // Set the initial value of the sort field order to whatever is saved.
        // The modified value will be saved when they click Apply.
        viewModelScope.launch {
            sortFieldOrder.value = sortKeyMapsUseCase.observeSortFieldOrder().first()
        }
    }

    fun swapSortPriority(fromIndex: Int, toIndex: Int) {
        sortFieldOrder.update {
            val newList = it.toMutableList()
            newList.add(toIndex, newList.removeAt(fromIndex))
            newList
        }
    }

    fun toggleSortOrder(field: SortField) {
        sortFieldOrder.update { sortFieldOrder ->
            val index = sortFieldOrder.indexOfFirst { it.field == field }

            sortFieldOrder.mapIndexed { i, sortFieldOrder ->
                if (i != index) {
                    return@mapIndexed sortFieldOrder
                }

                val newOrder = sortFieldOrder.order.toggle()
                sortFieldOrder.copy(order = newOrder)
            }
        }
    }

    fun resetSortPriority() {
        sortFieldOrder.update {
            it.map { sortFieldOrder ->
                sortFieldOrder.copy(order = SortOrder.NONE)
            }
        }
    }

    fun applySortPriority() {
        sortKeyMapsUseCase.setSortFieldOrder(sortFieldOrder.value)
    }

    fun setShowHelp(show: Boolean) {
        preferenceRepository.set(Keys.sortShowHelp, show)
    }

    fun showExample() {
        sortFieldOrder.value = listOf(
            SortFieldOrder(SortField.ACTIONS, SortOrder.ASCENDING),
            SortFieldOrder(SortField.TRIGGER, SortOrder.DESCENDING),
            SortFieldOrder(SortField.CONSTRAINTS),
            SortFieldOrder(SortField.OPTIONS),
        )
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
}
