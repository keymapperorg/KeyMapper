package io.github.sds100.keymapper.util

import androidx.lifecycle.MediatorLiveData
import java.util.*

/**
 * Created by sds100 on 13/01/21.
 */

class FilteredListLiveData<T : Searchable> : MediatorLiveData<DataState<List<T>>>() {
    init {
        value = Loading()
    }

    fun filter(models: DataState<List<T>>, query: String?) {
        value = Loading()

        value = when (models) {
            is Data -> {
                if (query == null) {
                    models
                } else {

                    val filteredList = models.data.filter { model ->
                        model.getSearchableString().toLowerCase(Locale.getDefault()).contains(query)
                    }

                    filteredList.getState()
                }
            }

            is Empty -> Empty()
            is Loading -> Loading()
        }
    }
}
