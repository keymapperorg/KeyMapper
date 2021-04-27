package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.util.ui.ISearchable
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.ui.createListState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Created by sds100 on 22/03/2021.
 */

suspend fun <T : ISearchable> List<T>.filterByQuery(query: String?): Flow<ListUiState<T>> = flow {

    if (query.isNullOrBlank()) {
        emit(ListUiState.Loaded(this@filterByQuery))
    } else {

        emit(ListUiState.Loading)

        val filteredList = withContext(Dispatchers.Default) {
            this@filterByQuery.filter { model ->
                model.getSearchableString().containsQuery(query)
            }
        }

        emit(filteredList.createListState())
    }
}

fun String.containsQuery(query: String?): Boolean {
    if (query.isNullOrBlank()) return true

    return toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))
}