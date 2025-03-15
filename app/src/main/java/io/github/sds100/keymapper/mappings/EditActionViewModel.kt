package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.actions.CreateActionUseCase
import io.github.sds100.keymapper.actions.CreateActionViewModel
import io.github.sds100.keymapper.actions.CreateActionViewModelImpl
import io.github.sds100.keymapper.actions.isEditable
import io.github.sds100.keymapper.mappings.keymaps.Action
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.ListItem
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.navigate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 12/04/2021.
 */

abstract class EditActionViewModel(
    private val resourceProvider: ResourceProvider,
    private val coroutineScope: CoroutineScope,
    private val configUseCase: ConfigKeyMapUseCase,
    createActionUseCase: CreateActionUseCase,
) : OptionsViewModel,
    CreateActionViewModel by CreateActionViewModelImpl(createActionUseCase, resourceProvider) {
    private val _actionUid = MutableStateFlow<String?>(null)
    val actionUid = _actionUid.asStateFlow()

    override val state = combine(configUseCase.keyMap, _actionUid) { mapping, actionUid ->

        when {
            mapping is State.Data -> {
                val action = mapping.data.actionList.find { it.uid == actionUid }
                    ?: return@combine EditActionUiState(showProgressBar = true)

                EditActionUiState(
                    listItems = createListItems(mapping.data, action),
                    showProgressBar = false,
                    isEditButtonVisible = action.data.isEditable(),
                )
            }

            else -> EditActionUiState(showProgressBar = true)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            EditActionUiState(showProgressBar = true),
        )

    fun setActionToConfigure(uid: String) {
        _actionUid.value = uid
    }

    fun onEditActionClick() {
        coroutineScope.launch {
            val actionUid = actionUid.value

            if (actionUid == null) {
                return@launch
            }

            val mappingState = configUseCase.keyMap.first()

            if (mappingState !is State.Data) {
                return@launch
            }

            val oldAction = mappingState.data.actionList
                .find { it.uid == actionUid } ?: return@launch

            val newActionData = editAction(oldAction.data)

            if (newActionData != null) {
                configUseCase.setActionData(actionUid, newActionData)
            }
        }
    }

    fun onReplaceActionClick() {
        coroutineScope.launch {
            val actionUid = actionUid.value ?: return@launch

            val newActionData =
                navigate("replace_action", NavDestination.ChooseAction) ?: return@launch

            configUseCase.setActionData(actionUid, newActionData)
        }
    }

    abstract fun createListItems(mapping: KeyMap, action: Action): List<ListItem>
}

data class EditActionUiState(
    override val showProgressBar: Boolean = false,
    override val listItems: List<ListItem> = emptyList(),
    val isEditButtonVisible: Boolean = false,
) : OptionsUiState
