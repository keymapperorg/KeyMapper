package io.github.sds100.keymapper.system.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.filterByQuery
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.SimpleListItem
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

/**
 * Created by sds100 on 27/01/2020.
 */

class ChooseAppViewModel constructor(
    private val useCase: DisplayAppsUseCase,
) : ViewModel() {

    val searchQuery = MutableStateFlow<String?>(null)

    private val showHiddenApps = MutableStateFlow(false)

    private val _state = MutableStateFlow(
        AppListState(
            State.Loading,
            showHiddenAppsButton = false,
            isHiddenAppsChecked = false
        )
    )
    val state = _state.asStateFlow()

    private val allAppListItems = useCase.installedPackages.map { state ->
        state.mapData { packages ->
            packages.buildListItems()
        }
    }.flowOn(Dispatchers.Default)

    private val launchableAppListItems = useCase.installedPackages.map { state ->
        state.mapData { packages ->
            packages
                .filter { it.isLaunchable }
                .buildListItems()
        }
    }.flowOn(Dispatchers.Default)

    private val _returnResult = MutableSharedFlow<String>()
    val returnResult = _returnResult.asSharedFlow()

    var allowHiddenApps: Boolean = false

    init {
        combine(
            allAppListItems,
            launchableAppListItems,
            showHiddenApps,
            searchQuery
        ) { allAppListItems, launchableAppListItems, showHiddenApps, query ->

            val packagesToFilter = if (allowHiddenApps && showHiddenApps) {
                allAppListItems
            } else {
                launchableAppListItems
            }

            when (packagesToFilter) {
                is State.Data -> {
                    packagesToFilter.data.filterByQuery(query).collectLatest { filteredListItems ->
                        _state.value = AppListState(
                            filteredListItems,
                            showHiddenAppsButton = allowHiddenApps,
                            isHiddenAppsChecked = showHiddenApps
                        )
                    }
                }

                is State.Loading -> _state.value =
                    AppListState(
                        State.Loading,
                        showHiddenAppsButton = allowHiddenApps,
                        isHiddenAppsChecked = showHiddenApps
                    )
            }
        }.launchIn(viewModelScope)
    }

    fun onHiddenAppsCheckedChange(checked: Boolean) {
        showHiddenApps.value = checked
    }

    fun onListItemClick(id: String) {
        viewModelScope.launch {
            val packageName = id

            _returnResult.emit(packageName)
        }
    }

    private suspend fun List<PackageInfo>.buildListItems(): List<SimpleListItem> = flow {
        forEach { packageInfo ->
            Timber.d("buildListItems::packageInfo %s", packageInfo.packageName)

            val name = useCase.getAppName(packageInfo.packageName)
                .valueOrNull() ?: return@forEach

            val icon = useCase.getAppIcon(packageInfo.packageName)
                .valueOrNull() ?: return@forEach

            val listItem = DefaultSimpleListItem(
                id = packageInfo.packageName,
                title = name,
                icon = IconInfo(icon)
            )

            emit(listItem)
        }
    }.flowOn(Dispatchers.Default)
        .toList()
        .sortedBy { it.title.lowercase(Locale.getDefault()) }

    class Factory(
        private val useCase: DisplayAppsUseCase
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ChooseAppViewModel(useCase) as T
    }
}

data class AppListState(
    val listItems: State<List<SimpleListItem>>,
    val showHiddenAppsButton: Boolean,
    val isHiddenAppsChecked: Boolean
)
