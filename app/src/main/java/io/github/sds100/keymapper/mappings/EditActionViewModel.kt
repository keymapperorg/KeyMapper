package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.actions.*
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 12/04/2021.
 */

abstract class EditActionViewModel<M : Mapping<A>, A : Action>(
    private val resourceProvider: ResourceProvider,
    private val coroutineScope: CoroutineScope,
    private val configUseCase: ConfigMappingUseCase<A, M>,
    createActionUseCase: CreateActionUseCase
) : OptionsViewModel,
    CreateActionViewModel by CreateActionViewModelImpl(createActionUseCase, resourceProvider) {
    private val _actionUid = MutableStateFlow<String?>(null)
    val actionUid = _actionUid.asStateFlow()

    override val state = combine(configUseCase.mapping, _actionUid) { mapping, actionUid ->

        when {
            mapping is State.Data -> {
                val action = mapping.data.actionList.find { it.uid == actionUid }
                    ?: return@combine EditActionUiState(showProgressBar = true)

                EditActionUiState(
                    listItems = createListItems(mapping.data, action),
                    showProgressBar = false,
                    isEditButtonVisible = action.data.isEditable()
                )
            }

            else -> EditActionUiState(showProgressBar = true)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            EditActionUiState(showProgressBar = true)
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

            val mappingState = configUseCase.mapping.first()

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

    abstract fun createListItems(mapping: M, action: A): List<ListItem>
}

data class EditActionUiState(
    override val showProgressBar: Boolean = false,
    override val listItems: List<ListItem> = emptyList(),
    val isEditButtonVisible: Boolean = false
) : OptionsUiState