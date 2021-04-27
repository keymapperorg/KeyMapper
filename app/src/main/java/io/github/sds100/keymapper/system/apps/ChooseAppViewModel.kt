package io.github.sds100.keymapper.system.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.filterByQuery
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * Created by sds100 on 27/01/2020.
 */

class ChooseAppViewModel internal constructor(
    private val useCase: DisplayAppsUseCase,
) : ViewModel() {

    val searchQuery = MutableStateFlow<String?>(null)

    private val showHiddenApps = MutableStateFlow(false)

    private val _state = MutableStateFlow(
        AppListState(
            ListUiState.Loading,
            showHiddenAppsButton = false,
            isHiddenAppsChecked = false
        )
    )
    val state = _state.asStateFlow()

    private val allAppListItems = useCase.installedPackages.map { state ->
        state.mapData { it.buildListItems() }
    }.flowOn(Dispatchers.Default)

    private val launchableAppListItems = useCase.installedPackages.map { state ->
        state.mapData { packageInfoList ->
            packageInfoList.filter { it.canBeLaunched }.buildListItems()
        }
    }.flowOn(Dispatchers.Default)

    init {

        combine(
            allAppListItems,
            launchableAppListItems,
            showHiddenApps,
            searchQuery
        ) { allAppListItems, launchableAppListItems, showHiddenApps, query ->

            val packagesToFilter = if (showHiddenApps) {
                allAppListItems
            } else {
                launchableAppListItems
            }

            when (packagesToFilter) {
                is State.Data -> {
                    packagesToFilter.data.filterByQuery(query).collectLatest { filteredListItems ->
                        _state.value = AppListState(
                            filteredListItems,
                            showHiddenAppsButton = true,
                            isHiddenAppsChecked = showHiddenApps
                        )
                    }
                }

                is State.Loading -> _state.value =
                    AppListState(
                        ListUiState.Loading,
                        showHiddenAppsButton = true,
                        isHiddenAppsChecked = showHiddenApps
                    )
            }
        }.launchIn(viewModelScope)
    }

    fun onHiddenAppsCheckedChange(checked: Boolean) {
        showHiddenApps.value = checked
    }

    private suspend fun List<PackageInfo>.buildListItems(): List<AppListItem> = flow {
        forEach {
            val name = useCase.getAppName(it.packageName).valueOrNull() ?: return@forEach
            val icon = useCase.getAppIcon(it.packageName).valueOrNull() ?: return@forEach

            val listItem = AppListItem(
                packageName = it.packageName,
                appName = name,
                icon = icon
            )

            emit(listItem)
        }
    }.flowOn(Dispatchers.Default)
        .toList()
        .sortedBy { it.appName.toLowerCase(Locale.getDefault()) }

    class Factory(
        private val useCase: DisplayAppsUseCase
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ChooseAppViewModel(useCase) as T
    }
}

data class AppListState(
    val listItems: ListUiState<AppListItem>,
    val showHiddenAppsButton: Boolean,
    val isHiddenAppsChecked: Boolean
)
