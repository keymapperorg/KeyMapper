package io.github.sds100.keymapper.system.apps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.containsQuery
import io.github.sds100.keymapper.util.ui.KMIcon
import io.github.sds100.keymapper.util.ui.SearchState
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

/**
 * Created by sds100 on 27/01/2020.
 */
@HiltViewModel
class ChooseActivityViewModel2 @Inject constructor(private val useCase: DisplayAppsUseCase) : ViewModel() {

    private val _searchState: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val allListItems: Flow<List<ChooseActivityListItem>> = useCase.installedPackages.dropWhile { it is State.Loading }.map { (it as State.Data).data }.map { createListItems(it) }.flowOn(Dispatchers.Default)

    var state: ChooseActivityState by mutableStateOf(ChooseActivityState.Loading)
        private set

    init {
        combine(allListItems, searchState) { allListItems, searchState ->
            val filteredListItems = when (searchState) {
                SearchState.Idle -> allListItems
                is SearchState.Searching -> allListItems.filter { listItem ->
                    listItem.packageName.containsQuery(searchState.query) || listItem.activityName.containsQuery(searchState.query)
                }
            }

            ChooseActivityState.Loaded(filteredListItems)
        }.onEach { state = it }.flowOn(Dispatchers.Default).launchIn(viewModelScope)
    }

    fun setSearchState(state: SearchState) {
        _searchState.value = state
    }

    private fun createListItems(packages: List<PackageInfo>): List<ChooseActivityListItem> {
        val listItems = sequence {
            packages.forEach { packageInfo ->

                val appName = useCase.getAppName(packageInfo.packageName).valueOrNull()
                    ?: return@forEach

                val appIcon = useCase.getAppIcon(packageInfo.packageName).valueOrNull()

                packageInfo.activities.forEach { activityInfo ->
                    val icon = appIcon?.let { KMIcon.Drawable(it) }
                    val listItem = ChooseActivityListItem(packageName = activityInfo.packageName, activityName = activityInfo.activityName, icon = icon)
                    yield(listItem)
                }
            }
        }.toList()

        return listItems.sortedBy { it.packageName.lowercase(Locale.getDefault()) }
    }
}

sealed class ChooseActivityState {
    object Loading : ChooseActivityState()
    data class Loaded(val listItems: List<ChooseActivityListItem>) : ChooseActivityState()
}

data class ChooseActivityListItem(val packageName: String, val activityName: String, val icon: KMIcon?)