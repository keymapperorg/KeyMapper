package io.github.sds100.keymapper.system.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.filterByQuery
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

/**
 * Created by sds100 on 27/01/2020.
 */
class ChooseActivityViewModel(private val useCase: DisplayAppsUseCase) : ViewModel() {

    val searchQuery = MutableStateFlow<String?>(null)

    private val unfilteredListItems: Flow<ListUiState<ActivityListItem>> =
        useCase.installedPackages.map { packagesState ->

            if (packagesState !is State.Data) {
                return@map ListUiState.Loading
            }

            val listItems = sequence {
                packagesState.data.forEach { packageInfo ->

                    val appName =
                        useCase.getAppName(packageInfo.packageName).valueOrNull() ?: return@forEach

                    val appIcon =
                        useCase.getAppIcon(packageInfo.packageName).valueOrNull()

                    packageInfo.activities.forEach { activityInfo ->
                        yield(
                            ActivityListItem(
                                appName = appName,
                                activityInfo = activityInfo,
                                icon = appIcon
                            )
                        )
                    }
                }
            }

            ListUiState.Loaded(listItems.toList().sortedBy { it.appName })
        }.flowOn(Dispatchers.Default)

    private val _listItems = MutableStateFlow<ListUiState<ActivityListItem>>(ListUiState.Loading)
    val listItems = _listItems.asStateFlow()


    init {
        combine(
            searchQuery,
            unfilteredListItems
        ) { searchQuery, listItems ->

            if (listItems is ListUiState.Loaded) {
                listItems.data.filterByQuery(searchQuery).collectLatest {
                    _listItems.value = it
                }
            } else {
                _listItems.value = listItems
            }

        }.launchIn(viewModelScope)
    }

    class Factory(
        private val useCase: DisplayAppsUseCase
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ChooseActivityViewModel(useCase) as T
    }
}
