package io.github.sds100.keymapper.util.ui

/**
 * Created by sds100 on 29/07/2022.
 */
sealed class SearchState {
    object Idle : SearchState()
    data class Searching(val query: String) : SearchState()
}
