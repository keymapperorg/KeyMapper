package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.data.model.AppShortcutListItemModel
import io.github.sds100.keymapper.data.repository.SystemRepository
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Created by sds100 on 27/01/2020.
 */
class AppShortcutListViewModel internal constructor(
    private val repository: SystemRepository
) : ViewModel() {

    val searchQuery: MutableLiveData<String> = MutableLiveData("")

    private val appShortcutModelList = liveData {
        emit(Loading())

        val appShortcutList = withContext(viewModelScope.coroutineContext + Dispatchers.Default) {
            repository.getAppShortcutList().map {
                //only include it if it has a configuration screen

                val name = repository.getIntentLabel(it)
                val icon = repository.getIntentIcon(it)

                AppShortcutListItemModel(it.activityInfo, name, icon)
            }.sortedBy { it.label.toLowerCase(Locale.getDefault()) }.getState()
        }

        emit(appShortcutList)
    }

    val filteredAppShortcutModelList = MediatorLiveData<State<List<AppShortcutListItemModel>>>().apply {
        fun filter(modelList: State<List<AppShortcutListItemModel>>, query: String) {
            value = Loading()

            when (modelList) {
                is Data -> {
                    val filteredList = modelList.data.filter { model ->
                        model.label.toLowerCase(Locale.getDefault()).contains(query)
                    }

                    value = filteredList.getState()
                }

                is Empty -> Empty()
                is Loading -> Loading()
            }
        }

        addSource(searchQuery) { query ->
            filter(appShortcutModelList.value ?: Empty(), query)
        }

        addSource(appShortcutModelList)
        {
            value = it

            searchQuery.value?.let { query ->
                filter(appShortcutModelList.value ?: Empty(), query)
            }
        }
    }

    class Factory(
        private val repository: SystemRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            AppShortcutListViewModel(repository) as T
    }
}
