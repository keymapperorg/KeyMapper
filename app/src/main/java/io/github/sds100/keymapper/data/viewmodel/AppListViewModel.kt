package io.github.sds100.keymapper.data.viewmodel

import android.content.pm.ApplicationInfo
import androidx.lifecycle.*
import io.github.sds100.keymapper.data.model.AppListItemModel
import io.github.sds100.keymapper.data.repository.PackageRepository
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.IModelState
import kotlinx.coroutines.Dispatchers
import java.util.*

/**
 * Created by sds100 on 27/01/2020.
 */
class AppListViewModel internal constructor(
    private val repository: PackageRepository
) : ViewModel(), IModelState<List<AppListItemModel>> {

    private val launchableAppModelList = liveData(viewModelScope.coroutineContext + Dispatchers.Default) {
        emit(Loading())

        emit(repository.getLaunchableAppList().createModels().getState())
    }

    private val allAppModelList = liveData(viewModelScope.coroutineContext + Dispatchers.Default) {
        emit(Loading())

        emit(repository.getAllAppList().createModels().getState())
    }

    val searchQuery: MutableLiveData<String> = MutableLiveData("")

    val showHiddenApps = MutableLiveData(false)

    val haveHiddenApps = launchableAppModelList.map {
        if (it is Data) {
            it.data.isNotEmpty()
        } else {
            false
        }
    }

    val _model = FilteredListLiveData<AppListItemModel>().apply {
        fun showAllApps() {
            value = allAppModelList.value

            filter(allAppModelList.value ?: Empty(), searchQuery.value)
        }

        fun showLaunchableApps() {
            value = launchableAppModelList.value

            filter(launchableAppModelList.value ?: Empty(), searchQuery.value)
        }

        addSource(searchQuery) { query ->
            if (showHiddenApps.value == true) {
                filter(allAppModelList.value ?: Empty(), query)
            } else {
                filter(launchableAppModelList.value ?: Empty(), query)
            }
        }

        addSource(allAppModelList) {
            if (showHiddenApps.value == true) {
                showAllApps()
            }
        }

        addSource(launchableAppModelList) {
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

    override val model = _model
    override val viewState = MutableLiveData<ViewState>(ViewLoading())

    private fun List<ApplicationInfo>.createModels(): List<AppListItemModel> =
        map {
            val name = repository.getAppName(it)
            val icon = repository.getAppIcon(it)

            AppListItemModel(it.packageName, name, icon)
        }.sortedBy { it.appName.toLowerCase(Locale.getDefault()) }

    class Factory(
        private val repository: PackageRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            AppListViewModel(repository) as T
    }
}
