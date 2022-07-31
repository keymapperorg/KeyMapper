package io.github.sds100.keymapper.system.apps

import android.content.Intent
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
class ChooseAppShortcutViewModel2 @Inject constructor(private val useCase: DisplayAppShortcutsUseCase) : ViewModel() {

    private val _searchState: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    var chooseState: ChooseAppShortcutState by mutableStateOf(ChooseAppShortcutState.Loading)
        private set

    var configState: ConfigAppShortcutState by mutableStateOf(ConfigAppShortcutState.Idle)
        private set

    private val listItems: Flow<List<ChooseAppShortcutListItem>> = useCase.shortcuts
        .dropWhile { it is State.Loading }
        .map { (it as State.Data).data }
        .map(this::createListItems)
        .flowOn(Dispatchers.Default)

    init {
        combine(
            searchState,
            listItems
        ) { searchState, listItems ->
            val filteredListItems = when (searchState) {
                SearchState.Idle -> listItems
                is SearchState.Searching -> listItems.filter { it.name.containsQuery(searchState.query) }
            }

            ChooseAppShortcutState.Loaded(filteredListItems)
        }
            .onEach { chooseState = it }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    @Suppress("DEPRECATION")
    fun onConfigShortcutResult(intent: Intent) {
        val intentShortcutName: String? = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)

        if (intentShortcutName != null) {
            val result = createResult(intent, intentShortcutName)
            configState = ConfigAppShortcutState.Finished(result)
        } else {
            configState = ConfigAppShortcutState.ChooseName(intent)
        }
    }

    fun onCreateShortcutName(name: String) {
        (configState as? ConfigAppShortcutState.ChooseName)?.intent?.let { intent ->
            val result = createResult(intent, name)
            configState = ConfigAppShortcutState.Finished(result)
        }
    }

    fun onDismissCreatingShortcutName() {
        configState = ConfigAppShortcutState.Idle
    }

    fun setSearchState(state: SearchState) {
        _searchState.value = state
    }

    @Suppress("DEPRECATION")
    private fun createResult(intent: Intent, shortcutName: String): ChooseAppShortcutResult {
        val uri: String

        if (intent.extras != null &&
            intent.extras!!.containsKey(Intent.EXTRA_SHORTCUT_INTENT)
        ) {
            //get intent from selected shortcut
            val shortcutIntent =
                intent.extras!!.get(Intent.EXTRA_SHORTCUT_INTENT) as Intent
            uri = shortcutIntent.toUri(0)

        } else {
            uri = intent.toUri(0)
        }

        val packageName = Intent.parseUri(uri, 0).`package`
            ?: intent.component?.packageName
            ?: Intent.parseUri(uri, 0).component?.packageName

        return ChooseAppShortcutResult(
            packageName = packageName,
            shortcutName = shortcutName,
            uri = uri
        )
    }

    private fun createListItems(shortcuts: List<AppShortcutInfo>): List<ChooseAppShortcutListItem> {
        return shortcuts
            .map(this::createListItem)
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    private fun createListItem(shortcutInfo: AppShortcutInfo): ChooseAppShortcutListItem {
        val icon: KMIcon = useCase.getShortcutIcon(shortcutInfo)
            .then { KMIcon.Drawable(it).success() }
            .valueOrNull() ?: KMIcon.ImageVector(Icons.Outlined.Android)

        val name = useCase.getShortcutName(shortcutInfo).valueOrNull() ?: shortcutInfo.packageName

        return ChooseAppShortcutListItem(shortcutInfo, name, icon)
    }
}

sealed class ConfigAppShortcutState {
    object Idle : ConfigAppShortcutState()
    data class ChooseName(val intent: Intent) : ConfigAppShortcutState()
    data class Finished(val result: ChooseAppShortcutResult) : ConfigAppShortcutState()
}

sealed class ChooseAppShortcutState {
    object Loading : ChooseAppShortcutState()
    data class Loaded(val listItems: List<ChooseAppShortcutListItem>) : ChooseAppShortcutState()
}

data class ChooseAppShortcutListItem(val shortcutInfo: AppShortcutInfo, val name: String, val icon: KMIcon)