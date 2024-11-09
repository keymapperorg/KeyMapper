package io.github.sds100.keymapper.sorting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.mappings.keymaps.KeyMapField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.util.Collections

class SortViewModel(
    observeKeyMapFieldSortOrderUseCase: ObserveKeyMapFieldSortOrderUseCase,
    observeKeyMapSortOrderUseCase: ObserveKeyMapSortOrderUseCase,
    private val toggleKeyMapFieldSortOrderUseCase: ToggleKeyMapFieldSortOrderUseCase,
    private val setKeyMapSortOrderUseCase: SetKeyMapSortOrderUseCase,
) : ViewModel() {
    val sortState = combine(
        observeKeyMapFieldSortOrderUseCase(KeyMapField.TRIGGER),
        observeKeyMapFieldSortOrderUseCase(KeyMapField.ACTIONS),
        observeKeyMapFieldSortOrderUseCase(KeyMapField.CONSTRAINTS),
        observeKeyMapFieldSortOrderUseCase(KeyMapField.OPTIONS),
        observeKeyMapSortOrderUseCase(),
    ) { trigger, actions, constraints, options, order ->
        if (fieldsOrder.isEmpty()) {
            fieldsOrder = order.toMutableList()
        }

        SortState(
            triggerSortOrder = trigger,
            actionsSortOrder = actions,
            constraintsSortOrder = constraints,
            optionsSortOrder = options,
            fieldsOrder = order,
        )
    }.flowOn(Dispatchers.Default)

    // Order of fields which are displayed in the UI
    private var fieldsOrder = mutableListOf<KeyMapField>()

    fun setLocalSortOrder(fromPosition: Int, toPosition: Int) {
        if (
            fromPosition < 0 || fromPosition >= fieldsOrder.size ||
            toPosition < 0 || toPosition >= fieldsOrder.size ||
            fromPosition == toPosition
        ) {
            return
        }

        Collections.swap(fieldsOrder, fromPosition, toPosition)
    }

    fun saveSortOrder() {
        if (fieldsOrder.isEmpty()) {
            return
        }

        setKeyMapSortOrderUseCase(fieldsOrder)
    }

    fun toggleField(field: KeyMapField) {
        toggleKeyMapFieldSortOrderUseCase(field)
    }

    class Factory(
        private val observeKeyMapFieldSortOrderUseCase: ObserveKeyMapFieldSortOrderUseCase,
        private val observeKeyMapSortOrderUseCase: ObserveKeyMapSortOrderUseCase,
        private val toggleKeyMapFieldSortOrderUseCase: ToggleKeyMapFieldSortOrderUseCase,
        private val setKeyMapSortOrderUseCase: SetKeyMapSortOrderUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            SortViewModel(
                observeKeyMapFieldSortOrderUseCase,
                observeKeyMapSortOrderUseCase,
                toggleKeyMapFieldSortOrderUseCase,
                setKeyMapSortOrderUseCase,
            ) as T
    }
}

data class SortState(
    val triggerSortOrder: SortOrder,
    val actionsSortOrder: SortOrder,
    val constraintsSortOrder: SortOrder,
    val optionsSortOrder: SortOrder,
    val fieldsOrder: List<KeyMapField>,
) {
    fun getSortOrder(field: KeyMapField) = when (field) {
        KeyMapField.TRIGGER -> triggerSortOrder
        KeyMapField.ACTIONS -> actionsSortOrder
        KeyMapField.CONSTRAINTS -> constraintsSortOrder
        KeyMapField.OPTIONS -> optionsSortOrder
    }
}
