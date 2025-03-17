package io.github.sds100.keymapper.mappings.keymaps

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.TintType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 08/09/20.
 */
class CreateKeyMapShortcutViewModel(
    private val configKeyMapUseCase: ConfigKeyMapUseCase,
    private val listKeyMaps: ListKeyMapsUseCase,
    private val createShortcutUseCase: CreateKeyMapShortcutUseCase,
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider {
    private val actionUiHelperOld = KeyMapActionUiHelperOld(listKeyMaps, resourceProvider)
    private val actionUiHelper = KeyMapActionUiHelper(listKeyMaps, resourceProvider)
    private val listItemCreator =
        KeyMapListItemCreator(actionUiHelper, listKeyMaps, resourceProvider)

    private val _state = MutableStateFlow<State<List<KeyMapListItemModel>>>(State.Loading)
    val state = _state.asStateFlow()

    private val _returnIntentResult = MutableSharedFlow<Intent>()
    val returnIntentResult = _returnIntentResult.asSharedFlow()

    var showShortcutNameDialog: String? by mutableStateOf(null)
    val shortcutNameDialogResult = MutableStateFlow<String?>(null)

    init {
        val rebuildUiState = MutableSharedFlow<State<List<KeyMap>>>(replay = 1)

        combine(
            rebuildUiState,
            listKeyMaps.showDeviceDescriptors,
        ) { keyMapListState, showDeviceDescriptors ->
            _state.value = keyMapListState.mapData { keyMapList ->
                keyMapList.map { keyMap ->
                    val keyMapListUiState = listItemCreator.create(keyMap, showDeviceDescriptors)

                    KeyMapListItemModel(isSelected = false, keyMapListUiState)
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            listKeyMaps.keyMapList.collectLatest {
                rebuildUiState.emit(it)
            }
        }

        viewModelScope.launch {
            listKeyMaps.invalidateActionErrors.drop(1).collectLatest {
                /*
                Don't get the key maps from the repository because there can be a race condition
                when restoring key maps. This happens because when the activity is resumed the
                key maps in the repository are being updated and this flow is collected
                at the same time.
                 */
                rebuildUiState.emit(rebuildUiState.first())
            }
        }
    }

    fun onKeyMapCardClick(uid: String) {
        viewModelScope.launch {
            val state = listKeyMaps.keyMapList.first { it is State.Data }

            if (state !is State.Data) return@launch

            configKeyMapUseCase.loadKeyMap(uid)
            configKeyMapUseCase.setTriggerFromOtherAppsEnabled(true)

            val keyMapState = configKeyMapUseCase.keyMap.first()

            if (keyMapState !is State.Data) return@launch

            val keyMap = keyMapState.data

            val key = "create_launcher_shortcut"
            val defaultShortcutName: String
            val icon: Drawable?

            if (keyMap.actionList.size == 1) {
                val action = keyMap.actionList.first().data
                defaultShortcutName = actionUiHelperOld.getTitle(
                    action,
                    showDeviceDescriptors = false,
                )

                val iconInfo = actionUiHelperOld.getIcon(action)

                if (iconInfo == null) {
                    icon = null
                } else {
                    when (iconInfo.tintType) {
                        // Always set the icon as black if it needs to be on surface because the
                        // background is white. Also, getting the colorOnSurface attribute
                        // from the application context doesn't seem to work correctly.
                        TintType.OnSurface -> iconInfo.drawable.setTint(Color.BLACK)
                        is TintType.Color -> iconInfo.drawable.setTint(iconInfo.tintType.color)
                        else -> {}
                    }

                    icon = iconInfo.drawable
                }
            } else {
                defaultShortcutName = ""
                icon = null
            }

            showShortcutNameDialog = defaultShortcutName

            val shortcutName = shortcutNameDialogResult.filterNotNull().first()
            shortcutNameDialogResult.value = null

            if (shortcutName.isBlank()) {
                return@launch
            }

            val intent = createShortcutUseCase.createIntent(
                keyMapUid = keyMap.uid,
                shortcutLabel = shortcutName,
                icon = icon,
            )

            configKeyMapUseCase.save()

            _returnIntentResult.emit(intent)
        }
    }

    class Factory(
        private val configKeyMapUseCase: ConfigKeyMapUseCase,
        private val listUseCase: ListKeyMapsUseCase,
        private val createShortcutUseCase: CreateKeyMapShortcutUseCase,
        private val resourceProvider: ResourceProvider,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = CreateKeyMapShortcutViewModel(
            configKeyMapUseCase,
            listUseCase,
            createShortcutUseCase,
            resourceProvider,
        ) as T
    }
}
