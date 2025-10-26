package io.github.sds100.keymapper.base.sorting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SortViewModel(
    private val coroutineScope: CoroutineScope,
    private val useCase: SortKeyMapsUseCase,
) {
    val showHelp: StateFlow<Boolean> =
        useCase.showHelp
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = true,
            )

    val sortFieldOrder: MutableStateFlow<List<SortFieldOrder>> = MutableStateFlow(emptyList())

    init {
        // Set the initial value of the sort field order to whatever is saved.
        // The modified value will be saved when they click Apply.
        coroutineScope.launch {
            sortFieldOrder.value = useCase.observeSortFieldOrder().first()
        }
    }

    fun swapSortPriority(
        fromIndex: Int,
        toIndex: Int,
    ) {
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
        sortFieldOrder.value = SortKeyMapsUseCaseImpl.defaultOrder
    }

    fun applySortPriority() {
        useCase.setSortFieldOrder(sortFieldOrder.value)
    }

    fun setShowHelp(show: Boolean) {
        useCase.setShowHelp(show)
    }

    fun showExample() {
        sortFieldOrder.value =
            listOf(
                SortFieldOrder(SortField.ACTIONS, SortOrder.ASCENDING),
                SortFieldOrder(SortField.TRIGGER, SortOrder.DESCENDING),
                SortFieldOrder(SortField.CONSTRAINTS),
                SortFieldOrder(SortField.OPTIONS),
            )
    }
}
