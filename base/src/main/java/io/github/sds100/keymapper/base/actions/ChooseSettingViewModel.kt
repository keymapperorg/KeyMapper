package io.github.sds100.keymapper.base.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.settings.SettingType
import io.github.sds100.keymapper.system.settings.SettingsAdapter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@HiltViewModel
class ChooseSettingViewModel @Inject constructor(
    private val settingsAdapter: SettingsAdapter,
    resourceProvider: ResourceProvider,
    navigationProvider: NavigationProvider,
    dialogProvider: DialogProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {
    val searchQuery = MutableStateFlow<String?>(null)

    val selectedSettingType = MutableStateFlow(SettingType.SYSTEM)
    val settings: StateFlow<State<List<SettingItem>>> =
        combine(selectedSettingType, searchQuery) { type, query ->
            val allSettings = settingsAdapter.getAll(type)

            val items = allSettings
                .filter { (key, _) -> query == null || key.contains(query, ignoreCase = true) }
                .map { (key, value) -> SettingItem(key, value) }

            State.Data(items)
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    fun onNavigateBack() {
        viewModelScope.launch {
            popBackStack()
        }
    }

    fun onSettingClick(key: String, currentValue: String?) {
        viewModelScope.launch {
            popBackStackWithResult(
                Json.encodeToString(
                    ChooseSettingResult.serializer(),
                    ChooseSettingResult(
                        settingType = selectedSettingType.value,
                        key = key,
                        currentValue = currentValue,
                    ),
                ),
            )
        }
    }
}

data class SettingItem(val key: String, val value: String?)

@Serializable
data class ChooseSettingResult(
    val settingType: SettingType,
    val key: String,
    val currentValue: String?,
)
