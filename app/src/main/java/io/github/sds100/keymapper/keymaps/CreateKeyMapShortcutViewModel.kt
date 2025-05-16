package io.github.sds100.keymapper.keymaps

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.actions.ActionErrorSnapshot
import io.github.sds100.keymapper.actions.ActionUiHelper
import io.github.sds100.keymapper.constraints.ConstraintErrorSnapshot
import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.constraints.ConstraintUiHelper
import io.github.sds100.keymapper.groups.GroupListItemModel
import io.github.sds100.keymapper.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.trigger.TriggerErrorSnapshot
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.TintType
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CreateKeyMapShortcutViewModel(
    private val configKeyMapUseCase: ConfigKeyMapUseCase,
    private val listKeyMaps: ListKeyMapsUseCase,
    private val createShortcutUseCase: CreateKeyMapShortcutUseCase,
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider {
    private val actionUiHelper = ActionUiHelper(listKeyMaps, resourceProvider)
    private val constraintUiHelper = ConstraintUiHelper(
        listKeyMaps,
        resourceProvider,
    )
    private val listItemCreator = KeyMapListItemCreator(listKeyMaps, resourceProvider)

    private val initialState = KeyMapListState(
        appBarState = KeyMapAppBarState.RootGroup(
            subGroups = emptyList(),
            warnings = emptyList(),
            isPaused = false,
        ),
        listItems = State.Loading,
    )
    private val _state: MutableStateFlow<KeyMapListState> = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    private val _returnIntentResult = MutableSharedFlow<Intent>()
    val returnIntentResult = _returnIntentResult.asSharedFlow()

    var showShortcutNameDialog: String? by mutableStateOf(null)
    val shortcutNameDialogResult = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            combine(
                listKeyMaps.keyMapGroup,
                listKeyMaps.showDeviceDescriptors,
                listKeyMaps.triggerErrorSnapshot,
                listKeyMaps.actionErrorSnapshot,
                listKeyMaps.constraintErrorSnapshot,
            ) { keyMapGroup, showDeviceDescriptors, triggerErrorSnapshot, actionErrorSnapshot, constraintErrorSnapshot ->
                _state.value = buildState(
                    keyMapGroup,
                    showDeviceDescriptors,
                    triggerErrorSnapshot,
                    actionErrorSnapshot,
                    constraintErrorSnapshot,
                )
            }.collect()
        }
    }

    private fun buildState(
        keyMapGroup: KeyMapGroup,
        showDeviceDescriptors: Boolean,
        triggerErrorSnapshot: TriggerErrorSnapshot,
        actionErrorSnapshot: ActionErrorSnapshot,
        constraintErrorSnapshot: ConstraintErrorSnapshot,
    ): KeyMapListState {
        val listItemsState = keyMapGroup.keyMaps.mapData { list ->
            list.map {
                val content = listItemCreator.build(
                    it,
                    showDeviceDescriptors,
                    triggerErrorSnapshot,
                    actionErrorSnapshot,
                    constraintErrorSnapshot,
                )

                KeyMapListItemModel(isSelected = false, content)
            }
        }

        val subGroupListItems = keyMapGroup.subGroups.map { group ->
            var icon: ComposeIconInfo? = null

            val constraint = group.constraintState.constraints.firstOrNull()
            if (constraint != null) {
                icon = constraintUiHelper.getIcon(constraint)
            }

            GroupListItemModel(
                uid = group.uid,
                name = group.name,
                icon = icon,
            )
        }

        val breadcrumbs = keyMapGroup.parents.plus(keyMapGroup.group).filterNotNull().map { group ->
            GroupListItemModel(
                uid = group.uid,
                name = group.name,
                icon = null,
            )
        }

        val appBarState = if (keyMapGroup.group == null) {
            KeyMapAppBarState.RootGroup(
                subGroups = subGroupListItems,
                warnings = emptyList(),
                isPaused = false,
            )
        } else {
            KeyMapAppBarState.ChildGroup(
                groupName = keyMapGroup.group.name,
                subGroups = subGroupListItems,
                constraints = emptyList(),
                constraintMode = ConstraintMode.AND,
                breadcrumbs = breadcrumbs,
                isEditingGroupName = false,
                isNewGroup = false,
                parentConstraintCount = keyMapGroup.parents.sumOf { it.constraintState.constraints.size },
            )
        }

        return KeyMapListState(appBarState, listItemsState)
    }

    fun onKeyMapCardClick(uid: String) {
        viewModelScope.launch {
            val state = listKeyMaps.keyMapGroup.first { it.keyMaps is State.Data }

            if (state.keyMaps !is State.Data) return@launch

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
                defaultShortcutName = actionUiHelper.getTitle(
                    action,
                    showDeviceDescriptors = false,
                )

                val iconInfo = actionUiHelper.getDrawableIcon(action)

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

    fun onGroupClick(uid: String?) {
        viewModelScope.launch {
            listKeyMaps.openGroup(uid)
        }
    }

    fun onPopGroupClick() {
        viewModelScope.launch {
            listKeyMaps.popGroup()
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
