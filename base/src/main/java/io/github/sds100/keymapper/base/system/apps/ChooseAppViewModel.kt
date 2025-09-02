package io.github.sds100.keymapper.base.system.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.filterByQuery
import io.github.sds100.keymapper.base.utils.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.base.utils.ui.IconInfo
import io.github.sds100.keymapper.base.utils.ui.SimpleListItemOld
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.system.apps.PackageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ChooseAppViewModel @Inject constructor(
    private val useCase: DisplayAppsUseCase,
) : ViewModel() {

    val searchQuery = MutableStateFlow<String?>(null)

    private val showHiddenApps = MutableStateFlow(false)

    private val _state = MutableStateFlow(
        AppListState(
            State.Loading,
            showHiddenAppsButton = false,
            isHiddenAppsChecked = false,
        ),
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
            searchQuery,
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
                            isHiddenAppsChecked = showHiddenApps,
                        )
                    }
                }

                is State.Loading ->
                    _state.value =
                        AppListState(
                            State.Loading,
                            showHiddenAppsButton = allowHiddenApps,
                            isHiddenAppsChecked = showHiddenApps,
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

    private suspend fun List<PackageInfo>.buildListItems(): List<SimpleListItemOld> = flow {
        forEach { packageInfo ->
            val name = useCase.getAppName(packageInfo.packageName)
                .valueOrNull() ?: return@forEach

            val icon = useCase.getAppIcon(packageInfo.packageName)
                .valueOrNull() ?: return@forEach

            val listItem = DefaultSimpleListItem(
                id = packageInfo.packageName,
                title = name,
                icon = IconInfo(icon),
            )

            emit(listItem)
        }
    }.flowOn(Dispatchers.Default)
        .toList()
        .sortedBy { it.title.lowercase(Locale.getDefault()) }
}

data class AppListState(
    val listItems: State<List<SimpleListItemOld>>,
    val showHiddenAppsButton: Boolean,
    val isHiddenAppsChecked: Boolean,
)
