package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.SystemRepository
import io.github.sds100.keymapper.data.model.AppListItemModel
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 27/01/2020.
 */
class AppListViewModel internal constructor(
        private val repository: SystemRepository
) : ViewModel(), ProgressCallback {

    val appModelList: MutableLiveData<List<AppListItemModel>> = MutableLiveData()

    override val loadingContent = MutableLiveData(false)

    init {
        viewModelScope.launch {
            loadingContent.value = true

            appModelList.value = repository.getAppList().map {
                val name = repository.getAppName(it) ?: it.packageName
                val icon = repository.getAppIcon(it)

                AppListItemModel(it.packageName, name, icon)
            }.sortedBy { it.appName }

            loadingContent.value = false
        }
    }

    class Factory(
            private val mRepository: SystemRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
                AppListViewModel(mRepository) as T
    }
}
