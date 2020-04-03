package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.data.model.SystemActionListItemModel
import io.github.sds100.keymapper.data.model.UnsupportedSystemActionListItemModel
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.SystemActionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import splitties.resources.appStr
import java.util.*

/**
 * Created by sds100 on 31/03/2020.
 */
class SystemActionListViewModel : ViewModel(), ProgressCallback {

    override val loadingContent = MutableLiveData(false)

    val searchQuery: MutableLiveData<String> = MutableLiveData("")

    private val mModelsSortedByCategory = liveData {
        loadingContent.value = true

        val systemActionsSortedByCategory = withContext(viewModelScope.coroutineContext + Dispatchers.Default) {
            val allModels = SystemActionUtils.getSupportedSystemActions().map {
                val requiresRoot = it.permissions.contains(Constants.PERMISSION_ROOT)

                SystemActionListItemModel(it.id, it.category, it.getDescription(), it.getIcon(), requiresRoot)
            }

            sequence {
                for ((categoryId, categoryLabel) in SystemActionUtils.CATEGORY_LABEL_MAP) {
                    val systemActions = allModels.filter { it.category == categoryId }

                    if (systemActions.isNotEmpty()) {
                        yield(appStr(categoryLabel) to systemActions)
                    }
                }
            }.toMap()
        }

        emit(systemActionsSortedByCategory)

        loadingContent.value = false
    }

    val filteredModelList = MediatorLiveData<Map<String, List<SystemActionListItemModel>>>().apply {
        fun filter(query: String) {
            mModelsSortedByCategory.value ?: return

            value = sequence {
                for ((category, systemActionList) in mModelsSortedByCategory.value!!) {
                    val matchedSystemActions = systemActionList.filter {
                        it.description.toLowerCase(Locale.getDefault()).contains(query)
                    }

                    if (matchedSystemActions.isNotEmpty()) {
                        yield(category to matchedSystemActions)
                    }
                }
            }.toMap()
        }

        addSource(searchQuery) { query ->
            filter(query)
        }

        addSource(mModelsSortedByCategory) {
            value = it

            searchQuery.value?.let { query ->
                filter(query)
            }
        }
    }

    val unsupportedSystemActions = liveData {
        val unsupportedActions = withContext(viewModelScope.coroutineContext + Dispatchers.Default) {
            SystemActionUtils.getUnsupportedSystemActionsWithReasons()
                .map {
                    val systemAction = it.key
                    val failure = it.value

                    UnsupportedSystemActionListItemModel(systemAction.id,
                        systemAction.getDescription(),
                        systemAction.getIcon(),
                        failure)
                }
        }

        emit(unsupportedActions)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SystemActionListViewModel() as T
        }
    }
}