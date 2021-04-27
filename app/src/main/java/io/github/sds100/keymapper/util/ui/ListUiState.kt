package io.github.sds100.keymapper.util.ui

/**
 * Created by sds100 on 17/03/2021.
 */
sealed class ListUiState<out T> {
    data class Loaded<out T>(val data: List<T>) : ListUiState<T>() {
        init {
            require(data.isNotEmpty()) { "List can't be empty!" }
        }
    }

    object Loading : ListUiState<Nothing>()
    object Empty : ListUiState<Nothing>()
}

fun <T> List<T>.createListState(): ListUiState<T> {
    return when {
        this.isEmpty() -> ListUiState.Empty
        else -> ListUiState.Loaded(this)
    }
}