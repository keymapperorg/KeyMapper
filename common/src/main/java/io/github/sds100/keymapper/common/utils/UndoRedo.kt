package io.github.sds100.keymapper.common.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UndoRedo<T> {
    private var history = mutableListOf<T>()
    private var index = -1

    private val _state = MutableStateFlow(UndoRedoState())
    val state: StateFlow<UndoRedoState> = _state.asStateFlow()

    fun record(value: T) {
        history = history.subList(0, index + 1).toMutableList()
        history.add(value)
        index = history.size - 1
        updateState()
    }

    fun undo(): T? {
        if (index <= 0) return null
        index--
        updateState()
        return history[index]
    }

    fun redo(): T? {
        if (index >= history.size - 1) return null
        index++
        updateState()
        return history[index]
    }

    fun reset(value: T) {
        history = mutableListOf(value)
        index = 0
        updateState()
    }

    private fun updateState() {
        _state.value = UndoRedoState(
            canUndo = index > 0,
            canRedo = index < history.size - 1,
        )
    }
}

data class UndoRedoState(val canUndo: Boolean = false, val canRedo: Boolean = false)
