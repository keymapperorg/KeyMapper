package io.github.sds100.keymapper.system.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.common.result.valueOrNull
import io.github.sds100.keymapper.common.state.State
import io.github.sds100.keymapper.util.filterByQuery
import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.TintType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ChooseActivityViewModel @Inject constructor(
    private val useCase: DisplayAppsUseCase
) : ViewModel() {

    val searchQuery = MutableStateFlow<String?>(null)

    private val unfilteredListItems: Flow<State<List<AppActivityListItem>>> =
        useCase.installedPackages.map { packagesState ->

            if (packagesState !is State.Data) {
                return@map State.Loading
            }

            val listItems = sequence {
                packagesState.data.forEach { packageInfo ->

                    val appName =
                        useCase.getAppName(packageInfo.packageName).valueOrNull() ?: return@forEach

                    val appIcon =
                        useCase.getAppIcon(packageInfo.packageName).valueOrNull()

                    packageInfo.activities.forEach { activityInfo ->
                        yield(
                            AppActivityListItem(
                                appName = appName,
                                activityInfo = activityInfo,
                                icon = appIcon?.let { IconInfo(it, TintType.None) },
                            ),
                        )
                    }
                }
            }

            val sortedListItems = listItems.toList().sortedBy { it.appName }

            return@map State.Data(sortedListItems)
        }.flowOn(Dispatchers.Default)

    private val _listItems = MutableStateFlow<State<List<AppActivityListItem>>>(State.Loading)
    val listItems = _listItems.asStateFlow()

    init {
        combine(
            searchQuery,
            unfilteredListItems,
        ) { searchQuery, unfilteredListItemsState ->

            if (unfilteredListItemsState is State.Data) {
                unfilteredListItemsState.data.filterByQuery(searchQuery).collectLatest {
                    _listItems.value = it
                }
            } else {
                _listItems.value = unfilteredListItemsState
            }
        }.flowOn(Dispatchers.Default).launchIn(viewModelScope)
    }
}
