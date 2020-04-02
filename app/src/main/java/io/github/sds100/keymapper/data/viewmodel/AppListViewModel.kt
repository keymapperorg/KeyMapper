package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.data.SystemRepository
import io.github.sds100.keymapper.data.model.AppListItemModel
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by sds100 on 27/01/2020.
 */
class AppListViewModel internal constructor(
    private val repository: SystemRepository
) : ViewModel(), ProgressCallback {

    val searchQuery: MutableLiveData<String> = MutableLiveData("")

    private val mAppModelList = liveData {
        loadingContent.value = true

        val modelList = repository.getAppList().map {
            val name = repository.getAppName(it) ?: it.packageName
            val icon = repository.getAppIcon(it)

            AppListItemModel(it.packageName, name, icon)
        }.sortedBy { it.appName.toLowerCase(Locale.getDefault()) }

        emit(modelList)

        loadingContent.value = false
    }

    val filteredAppModelList = MediatorLiveData<List<AppListItemModel>>().apply {
        fun filter(query: String) {
            value = mAppModelList.value?.filter {
                it.appName.toLowerCase(Locale.getDefault()).contains(query)
            } ?: listOf()
        }

        addSource(searchQuery) { query ->
            filter(query)
        }

        addSource(mAppModelList) {
            value = it

            searchQuery.value?.let { query ->
                filter(query)
            }
        }
    }

    override val loadingContent = MutableLiveData(false)

    class Factory(
        private val mRepository: SystemRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            AppListViewModel(mRepository) as T
    }
}
