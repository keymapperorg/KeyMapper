package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.model.ActivityListItemModel
import io.github.sds100.keymapper.data.repository.PackageRepository
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.IModelState
import io.github.sds100.keymapper.util.result.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 27/01/2020.
 */
class ActivityListViewModel internal constructor(
    private val repository: PackageRepository
) : ViewModel(), IModelState<List<ActivityListItemModel>> {

    val searchQuery: MutableLiveData<String> = MutableLiveData("")

    private val allModels = MutableLiveData<DataState<List<ActivityListItemModel>>>(Loading())

    override val model = FilteredListLiveData<ActivityListItemModel>().apply {
        addSource(searchQuery) { query ->
            filter(allModels.value ?: Empty(), query)
        }

        addSource(allModels) {
            value = it

            filter(it, searchQuery.value)
        }
    }

    override val viewState = MutableLiveData<ViewState>(ViewLoading())

    fun rebuildModels() {
        viewModelScope.launch(Dispatchers.Default) {
            flow {
                val appList = repository.getAllAppList()

                appList.forEach { appInfo ->
                    val packageName = appInfo.packageName

                    repository.getActivitiesForPackage(packageName).onSuccess { activityInfoList ->
                        activityInfoList.forEach {
                            emit(
                                ActivityListItemModel(
                                    appName = repository.getAppName(packageName),
                                    activityInfo = it,
                                    icon = repository.getAppIcon(appInfo)
                                )
                            )
                        }
                    }
                }
            }.toList(mutableListOf()).let { list ->
                withContext(Dispatchers.Main) {
                    allModels.value = list.sortedBy { it.appName }.getState()
                }
            }
        }
    }

    class Factory(
        private val repository: PackageRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ActivityListViewModel(repository) as T
    }
}
