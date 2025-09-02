package io.github.sds100.keymapper.base.utils

import io.github.sds100.keymapper.base.utils.ui.ISearchable
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Locale

fun <T : ISearchable> List<T>.filterByQuery(query: String?): Flow<State<List<T>>> = flow {
    if (query.isNullOrBlank()) {
        emit(State.Data(this@filterByQuery))
    } else {
        emit(State.Loading)

        val filteredList = withContext(Dispatchers.Default) {
            this@filterByQuery.filter { model ->
                model.getSearchableString().containsQuery(query)
            }
        }

        emit(State.Data(filteredList))
    }
}

fun String.containsQuery(query: String?): Boolean {
    if (query.isNullOrBlank()) return true

    return lowercase(Locale.getDefault()).contains(query.trim().lowercase(Locale.getDefault()))
}
