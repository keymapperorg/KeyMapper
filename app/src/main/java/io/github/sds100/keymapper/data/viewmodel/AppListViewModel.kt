package io.github.sds100.keymapper.data.viewmodel

import android.content.pm.ApplicationInfo
import androidx.lifecycle.*
import io.github.sds100.keymapper.data.SystemRepository
import io.github.sds100.keymapper.data.model.AppListItemModel
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Created by sds100 on 27/01/2020.
 */
class AppListViewModel internal constructor(
    private val repository: SystemRepository
) : ViewModel(), ProgressCallback {

    private val mLaunchableAppModelList = liveData {
        emit(repository.getLaunchableAppList().createModels())
    }

    private val mAllAppModelList = liveData {
        emit(repository.getAllAppList().createModels())
    }

    val searchQuery: MutableLiveData<String> = MutableLiveData("")

    val showHiddenApps = MutableLiveData<Boolean>(false)

    val haveHiddenApps = mLaunchableAppModelList.map {
        it.isNotEmpty()
    }

    val filteredAppModelList = MediatorLiveData<List<AppListItemModel>>().apply {
        fun filter(modelList: List<AppListItemModel>, query: String) {
            value = modelList.filter {
                it.appName.toLowerCase(Locale.getDefault()).contains(query)
            }
        }

        fun showAllApps() {
            value = mAllAppModelList.value

            searchQuery.value?.let { query ->
                filter(mAllAppModelList.value ?: listOf(), query)
            }

            loadingContent.value = false
        }

        fun showLaunchableApps() {
            value = mLaunchableAppModelList.value

            searchQuery.value?.let { query ->
                filter(mLaunchableAppModelList.value ?: listOf(), query)
            }

            loadingContent.value = false
        }

        addSource(searchQuery) { query ->
            if (showHiddenApps.value == true) {
                filter(mAllAppModelList.value ?: listOf(), query)
            } else {
                filter(mLaunchableAppModelList.value ?: listOf(), query)
            }
        }

        addSource(mAllAppModelList) {
            if (showHiddenApps.value == true) {
                showAllApps()
            }
        }

        addSource(mLaunchableAppModelList) {
            if (showHiddenApps.value == false) {
                showLaunchableApps()
            }
        }

        addSource(showHiddenApps) {
            if (it == true) {
                showAllApps()
            } else {
                showLaunchableApps()
            }
        }
    }

    override val loadingContent = MutableLiveData(true)

    private suspend fun List<ApplicationInfo>.createModels(): List<AppListItemModel> =
        withContext(viewModelScope.coroutineContext + Dispatchers.Default) {
            return@withContext map {
                val name = repository.getAppName(it) ?: it.packageName
                val icon = repository.getAppIcon(it)

                AppListItemModel(it.packageName, name, icon)
            }.sortedBy { it.appName.toLowerCase(Locale.getDefault()) }
        }

    class Factory(
        private val mRepository: SystemRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            AppListViewModel(mRepository) as T
    }
}
