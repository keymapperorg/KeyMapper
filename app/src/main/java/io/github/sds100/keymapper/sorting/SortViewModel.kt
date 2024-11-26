package io.github.sds100.keymapper.sorting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SortViewModel(
    private val setKeyMapFieldSortOrderUseCase: SetKeyMapFieldSortOrderUseCase,
    private val observeKeyMapFieldSortOrderUseCase: ObserveKeyMapFieldSortOrderUseCase,
) : ViewModel() {
    val triggerSortState = MutableStateFlow(SortOrder.NONE)
    val actionsSortState = MutableStateFlow(SortOrder.NONE)
    val constraintsSortState = MutableStateFlow(SortOrder.NONE)
    val optionsSortState = MutableStateFlow(SortOrder.NONE)

    init {
        viewModelScope.launch {
            triggerSortState.emit(observeKeyMapFieldSortOrderUseCase(SortField.TRIGGER).first())
            actionsSortState.emit(observeKeyMapFieldSortOrderUseCase(SortField.ACTIONS).first())
            constraintsSortState.emit(observeKeyMapFieldSortOrderUseCase(SortField.CONSTRAINTS).first())
            optionsSortState.emit(observeKeyMapFieldSortOrderUseCase(SortField.OPTIONS).first())
        }
    }

    fun toggleSortOrder(field: SortField) {
        viewModelScope.launch {
            when (field) {
                SortField.TRIGGER -> triggerSortState.emit(triggerSortState.value.toggle())
                SortField.ACTIONS -> actionsSortState.emit(actionsSortState.value.toggle())
                SortField.CONSTRAINTS -> constraintsSortState.emit(constraintsSortState.value.toggle())
                SortField.OPTIONS -> optionsSortState.emit(optionsSortState.value.toggle())
            }
        }
    }

    fun applySortOrder() {
        viewModelScope.launch {
            setKeyMapFieldSortOrderUseCase(SortField.TRIGGER, triggerSortState.value)
            setKeyMapFieldSortOrderUseCase(SortField.ACTIONS, actionsSortState.value)
            setKeyMapFieldSortOrderUseCase(SortField.CONSTRAINTS, constraintsSortState.value)
            setKeyMapFieldSortOrderUseCase(SortField.OPTIONS, optionsSortState.value)
        }
    }

    fun resetSortOrder() {
        viewModelScope.launch {
            triggerSortState.emit(SortOrder.NONE)
            actionsSortState.emit(SortOrder.NONE)
            constraintsSortState.emit(SortOrder.NONE)
            optionsSortState.emit(SortOrder.NONE)
        }
    }

    // If user explicitly cancels, restore the state that was saved when the dialog was opened
    private var checkpoint: Checkpoint? = null

    fun saveCheckpoint() {
        checkpoint = Checkpoint()
    }

    fun restoreCheckpoint() {
        checkpoint?.restore()
    }

    class Factory(
        private val setKeyMapFieldSortOrderUseCase: SetKeyMapFieldSortOrderUseCase,
        private val observeKeyMapFieldSortOrderUseCase: ObserveKeyMapFieldSortOrderUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            SortViewModel(
                setKeyMapFieldSortOrderUseCase,
                observeKeyMapFieldSortOrderUseCase,
            ) as T
    }

    private inner class Checkpoint(
        val triggerSortState: SortOrder = this@SortViewModel.triggerSortState.value,
        val actionsSortState: SortOrder = this@SortViewModel.actionsSortState.value,
        val constraintsSortState: SortOrder = this@SortViewModel.constraintsSortState.value,
        val optionsSortState: SortOrder = this@SortViewModel.optionsSortState.value,
    ) {
        fun restore() {
            viewModelScope.launch {
                this@SortViewModel.triggerSortState.emit(triggerSortState)
                this@SortViewModel.actionsSortState.emit(actionsSortState)
                this@SortViewModel.constraintsSortState.emit(constraintsSortState)
                this@SortViewModel.optionsSortState.emit(optionsSortState)
            }
        }
    }
}
