package io.github.sds100.keymapper.system.apps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.KMIcon
import io.github.sds100.keymapper.util.ui.SearchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

/**
 * Created by sds100 on 30/07/2022.
 */
@HiltViewModel
class ChooseAppViewModel2 @Inject constructor(private val useCase: DisplayAppsUseCase) : ViewModel() {

    private val _searchState: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val allAppListItems: Flow<List<ChooseAppListItem>> =
        useCase.installedPackages
            .dropWhile { it is State.Loading }
            .map { (it as State.Data).data }
            .map { packages -> createListItems(packages) }
            .flowOn(Dispatchers.Default)

    private val launchableAppListItems: Flow<List<ChooseAppListItem>> =
        useCase.installedPackages
            .dropWhile { it is State.Loading }
            .map { (it as State.Data).data }
            .map { packages ->
                packages
                    .filter { it.isLaunchable }
                    .let { createListItems(it) }
            }.flowOn(Dispatchers.Default)

    var state: ChooseAppState by mutableStateOf(ChooseAppState.Loading)
        private set

    private val showHiddenApps: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        combine(
            allAppListItems,
            launchableAppListItems,
            showHiddenApps,
            searchState
        ) { allAppListItems, launchableAppListItems, showHiddenApps, searchState ->
            val listItemsToFilter = if (showHiddenApps) {
                allAppListItems
            } else {
                launchableAppListItems
            }

            val filteredListItems = when (searchState) {
                SearchState.Idle -> listItemsToFilter
                is SearchState.Searching -> listItemsToFilter.filter { it.name.containsQuery(searchState.query) }
            }

            ChooseAppState.Loaded(filteredListItems, showHiddenApps)
        }
            .onEach { state = it }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    fun setSearchState(state: SearchState) {
        _searchState.value = state
    }

    fun onHiddenAppsCheckedChange(isChecked: Boolean) {
        showHiddenApps.value = isChecked
    }

    private fun createListItems(packages: List<PackageInfo>): List<ChooseAppListItem> {
        return packages
            .map(this::createListItem)
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    private fun createListItem(packageInfo: PackageInfo): ChooseAppListItem {
        val icon: KMIcon = useCase.getAppIcon(packageInfo.packageName)
            .then { KMIcon.Drawable(it).success() }
            .valueOrNull() ?: KMIcon.ImageVector(Icons.Outlined.Android)

        val name = useCase.getAppName(packageInfo.packageName).valueOrNull() ?: packageInfo.packageName

        return ChooseAppListItem(
            packageName = packageInfo.packageName,
            name = name,
            icon = icon
        )
    }
}

sealed class ChooseAppState {
    object Loading : ChooseAppState()
    data class Loaded(
        val listItems: List<ChooseAppListItem>,
        val isHiddenAppsChecked: Boolean
    ) : ChooseAppState()
}

data class ChooseAppListItem(val packageName: String, val name: String, val icon: KMIcon)