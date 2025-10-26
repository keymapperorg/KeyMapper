package io.github.sds100.keymapper.base.shortcuts

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.actions.ActionErrorSnapshot
import io.github.sds100.keymapper.base.actions.ActionUiHelper
import io.github.sds100.keymapper.base.constraints.ConstraintErrorSnapshot
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.constraints.ConstraintUiHelper
import io.github.sds100.keymapper.base.groups.GroupListItemModel
import io.github.sds100.keymapper.base.home.KeyMapAppBarState
import io.github.sds100.keymapper.base.home.KeyMapGroup
import io.github.sds100.keymapper.base.home.KeyMapListItemCreator
import io.github.sds100.keymapper.base.home.KeyMapListState
import io.github.sds100.keymapper.base.home.ListKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapState
import io.github.sds100.keymapper.base.trigger.ConfigTriggerUseCase
import io.github.sds100.keymapper.base.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.base.trigger.TriggerErrorSnapshot
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.TintType
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.mapData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateKeyMapShortcutViewModel
    @Inject
    constructor(
        private val configKeyMapState: ConfigKeyMapState,
        private val configTrigger: ConfigTriggerUseCase,
        private val listKeyMaps: ListKeyMapsUseCase,
        private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
        private val resourceProvider: ResourceProvider,
    ) : ViewModel() {
        private val actionUiHelper = ActionUiHelper(listKeyMaps, resourceProvider)
        private val constraintUiHelper =
            ConstraintUiHelper(
                listKeyMaps,
                resourceProvider,
            )
        private val listItemCreator = KeyMapListItemCreator(listKeyMaps, resourceProvider)

        private val initialState =
            KeyMapListState(
                appBarState =
                    KeyMapAppBarState.RootGroup(
                        subGroups = emptyList(),
                        warnings = emptyList(),
                        isPaused = false,
                    ),
                listItems = State.Loading,
                showCreateKeyMapTapTarget = false,
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
                    _state.value =
                        buildState(
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
            val listItemsState =
                keyMapGroup.keyMaps.mapData { list ->
                    list.map {
                        val content =
                            listItemCreator.build(
                                it,
                                showDeviceDescriptors,
                                triggerErrorSnapshot,
                                actionErrorSnapshot,
                                constraintErrorSnapshot,
                            )

                        KeyMapListItemModel(isSelected = false, content)
                    }
                }

            val subGroupListItems =
                keyMapGroup.subGroups.map { group ->
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

            val breadcrumbs =
                keyMapGroup.parents.plus(keyMapGroup.group).filterNotNull().map { group ->
                    GroupListItemModel(
                        uid = group.uid,
                        name = group.name,
                        icon = null,
                    )
                }

            val appBarState =
                if (keyMapGroup.group == null) {
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
                        keyMapsEnabled = null,
                    )
                }

            return KeyMapListState(appBarState, listItemsState, showCreateKeyMapTapTarget = false)
        }

        fun onKeyMapCardClick(uid: String) {
            viewModelScope.launch {
                val state = listKeyMaps.keyMapGroup.first { it.keyMaps is State.Data }

                if (state.keyMaps !is State.Data) return@launch

                configKeyMapState.loadKeyMap(uid)
                configTrigger.setTriggerFromOtherAppsEnabled(true)

                val keyMapState = configKeyMapState.keyMap.first()

                if (keyMapState !is State.Data) return@launch

                val keyMap = keyMapState.data

                "create_launcher_shortcut"
                val defaultShortcutName: String
                val icon: Drawable?

                if (keyMap.actionList.size == 1) {
                    val action = keyMap.actionList.first().data
                    defaultShortcutName =
                        actionUiHelper.getTitle(
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

                val intent =
                    createKeyMapShortcut.createIntent(
                        keyMapUid = keyMap.uid,
                        shortcutLabel = shortcutName,
                        icon = icon,
                    )

                configKeyMapState.save()

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
    }
