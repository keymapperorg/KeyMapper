package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.data.model.SystemActionListItemModel
import io.github.sds100.keymapper.data.model.UnsupportedSystemActionListItemModel
import io.github.sds100.keymapper.data.repository.SystemActionRepository
import io.github.sds100.keymapper.ui.callback.StringResourceProvider
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Created by sds100 on 31/03/2020.
 */
class SystemActionListViewModel(
    private val repository: SystemActionRepository,
    private var stringResourceProvider: StringResourceProvider? = null
) : ViewModel() {

    val searchQuery = MutableLiveData("")

    private val modelsSortedByCategory = liveData {

        emit(Loading())

        val systemActionsSortedByCategory = withContext(viewModelScope.coroutineContext + Dispatchers.Default) {
            val allModels = repository.supportedSystemActions.map {
                val requiresRoot = it.permissions.contains(Constants.PERMISSION_ROOT)

                SystemActionListItemModel(it.id, it.category, it.descriptionRes, it.iconRes, requiresRoot)
            }

            sequence {
                for ((categoryId, categoryLabel) in SystemActionUtils.CATEGORY_LABEL_MAP) {
                    val systemActions = allModels.filter { it.categoryId == categoryId }

                    if (systemActions.isNotEmpty()) {
                        yield(categoryLabel to systemActions)
                    }
                }
            }.toMap().getState()
        }

        emit(systemActionsSortedByCategory)
    }

    val filteredModelList = MediatorLiveData<State<Map<Int, List<SystemActionListItemModel>>>>().apply {
        fun filter(query: String) {
            modelsSortedByCategory.value ?: return
            stringResourceProvider ?: return

            value = Loading()

            modelsSortedByCategory.value?.let { modelsSortedByCategory ->
                if (modelsSortedByCategory is Data) {
                    val filteredModels = sequence {
                        for ((category, systemActionList) in modelsSortedByCategory.data) {
                            val matchedSystemActions = systemActionList.filter {
                                val descriptionString = stringResourceProvider!!.getStringResource(it.descriptionRes)

                                descriptionString.toLowerCase(Locale.getDefault()).contains(query)
                            }

                            if (matchedSystemActions.isNotEmpty()) {
                                yield(category to matchedSystemActions)
                            }
                        }
                    }.toMap()

                    value = filteredModels.getState()
                }
            }
        }

        addSource(searchQuery) { query ->
            filter(query)
        }

        addSource(modelsSortedByCategory) {
            value = it

            searchQuery.value?.let { query ->
                filter(query)
            }
        }
    }

    val unsupportedSystemActions = liveData {
        emit(Loading())

        val unsupportedActions = withContext(viewModelScope.coroutineContext + Dispatchers.Default) {
            repository.unsupportedSystemActions.map {
                val systemAction = it.key
                val failure = it.value

                UnsupportedSystemActionListItemModel(systemAction.id,
                    systemAction.descriptionRes,
                    systemAction.iconRes,
                    failure)
            }.getState()
        }

        emit(unsupportedActions)
    }

    fun registerStringResourceProvider(stringResourceProvider: StringResourceProvider) {
        this.stringResourceProvider = stringResourceProvider
    }

    fun unregisterStringResourceProvider() {
        stringResourceProvider = null
    }

    override fun onCleared() {
        super.onCleared()

        unregisterStringResourceProvider()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val systemActionRepository: SystemActionRepository) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SystemActionListViewModel(systemActionRepository) as T
        }
    }
}