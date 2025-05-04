package io.github.sds100.keymapper.actions.uielement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.system.accessibility.RecordAccessibilityNodeState
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.containsQuery
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.otherwise
import io.github.sds100.keymapper.util.then
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.ViewModelHelper
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.util.ui.compose.SimpleListItemModel
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InteractUiElementViewModel(
    private val useCase: InteractUiElementUseCase,
    private val resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl() {

    private val _returnAction: MutableSharedFlow<ActionData.InteractUiElement> = MutableSharedFlow()
    val returnAction: SharedFlow<ActionData.InteractUiElement> = _returnAction

    val recordState: StateFlow<State<RecordUiElementState>> = combine(
        useCase.recordState,
        useCase.interactionCount,
    ) { recordState, interactionCountState ->
        val interactionCount = interactionCountState.dataOrNull() ?: return@combine State.Loading

        when (recordState) {
            is RecordAccessibilityNodeState.CountingDown -> {
                val mins = recordState.timeLeft / 60
                val secs = recordState.timeLeft % 60

                State.Data(
                    RecordUiElementState.CountingDown(
                        timeRemaining = "$mins:$secs",
                        interactionCount = interactionCount,
                    ),
                )
            }

            RecordAccessibilityNodeState.Idle -> {
                if (interactionCount == 0) {
                    State.Data(RecordUiElementState.Empty)
                } else {
                    State.Data(RecordUiElementState.Recorded(interactionCount = interactionCount))
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, State.Loading)

    private val _selectedElementState = MutableStateFlow<SelectedUiElementState?>(null)
    val selectedElementState: StateFlow<SelectedUiElementState?> =
        _selectedElementState.asStateFlow()

    val appSearchQuery = MutableStateFlow<String?>(null)

    private val appListItems: Flow<State<List<SimpleListItemModel>>> = useCase.interactedPackages
        .map { state -> state.mapData { list -> list.map(::buildInteractedPackageListItem) } }

    val filteredAppListItems = combine(
        appListItems,
        appSearchQuery,
    ) { state, query ->
        state.mapData { listItems ->
            listItems.filter { model ->
                model.title.containsQuery(query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, State.Loading)

    private val selectedApp = MutableStateFlow<String?>(null)

    val elementSearchQuery = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val interactionsByPackage: StateFlow<State<List<AccessibilityNodeEntity>>> = selectedApp
        .filterNotNull()
        .flatMapLatest { packageName -> useCase.getInteractionsByPackage(packageName) }
        .stateIn(viewModelScope, SharingStarted.Lazily, State.Loading)

    private val elementListItems: Flow<State<List<UiElementListItemModel>>> = interactionsByPackage
        .map { state -> state.mapData { list -> list.map(::buildUiElementListItem) } }

    private val interactionTypesFilterItems: Flow<State<List<Pair<NodeInteractionType?, String>>>> =
        interactionsByPackage
            .map { state -> state.mapData(::buildInteractionTypeFilterItems) }

    private val selectedInteractionTypeFilter = MutableStateFlow<NodeInteractionType?>(null)

    private val filteredElementListItems = combine(
        elementListItems,
        elementSearchQuery,
        selectedInteractionTypeFilter,
    ) { state, query, interactionType ->
        state.mapData { listItems ->
            listItems.filter { model ->
                if (interactionType != null && !model.interactionTypes.contains(interactionType)) {
                    return@filter false
                }

                val modelString = buildString {
                    append(model.nodeText)
                    append(" ")
                    append(model.nodeClassName)
                    append(" ")
                    append(model.nodeViewResourceId)
                }
                modelString.containsQuery(query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, State.Loading)

    val selectUiElementState: StateFlow<State<SelectUiElementState>> = combine(
        filteredElementListItems,
        interactionTypesFilterItems,
        selectedInteractionTypeFilter,
    ) { listItemsState, interactionTypesState, selectedInteractionType ->
        val listItems = listItemsState.dataOrNull() ?: return@combine State.Loading
        val interactionTypes = interactionTypesState.dataOrNull() ?: return@combine State.Loading

        val newState = SelectUiElementState(
            listItems = listItems,
            interactionTypes = interactionTypes,
            selectedInteractionType = selectedInteractionType,
        )
        State.Data(newState)
    }.stateIn(viewModelScope, SharingStarted.Lazily, State.Loading)

    fun loadAction(action: ActionData.InteractUiElement) {
        viewModelScope.launch {
            val appName = useCase.getAppName(action.packageName).valueOrNull() ?: action.packageName
            val appIcon = getAppIcon(action.packageName)

            val newState = SelectedUiElementState(
                description = action.description,
                appName = appName,
                appIcon = appIcon,
                nodeText = action.text ?: action.contentDescription,
                nodeClassName = action.className,
                nodeViewResourceId = action.viewResourceId,
                nodeUniqueId = action.uniqueId,
                interactionTypes = action.nodeActions,
                selectedInteraction = action.nodeAction,
            )

            _selectedElementState.update { newState }
        }
    }

    fun onDoneClick() {
        val selectedElementState = _selectedElementState.value
        if (selectedElementState == null) {
            return
        }

        val action = ActionData.InteractUiElement(
            description = selectedElementState.description,
            nodeAction = selectedElementState.selectedInteraction,
            packageName = selectedElementState.appName,
            text = selectedElementState.nodeText,
            contentDescription = selectedElementState.nodeText,
            className = selectedElementState.nodeClassName,
            viewResourceId = selectedElementState.nodeViewResourceId,
            uniqueId = selectedElementState.nodeUniqueId,
            nodeActions = selectedElementState.interactionTypes,
        )

        _returnAction.tryEmit(action)
    }

    fun onRecordClick() {
        recordState.value.ifIsData { recordState ->
            viewModelScope.launch {
                when (recordState) {
                    is RecordUiElementState.CountingDown -> useCase.stopRecording()
                    RecordUiElementState.Empty -> startRecording()
                    is RecordUiElementState.Recorded -> startRecording()
                }
            }
        }
    }

    fun onSelectApp(packageName: String) {
        elementSearchQuery.update { null }
        selectedApp.update { packageName }
    }

    fun onSelectElement(id: Long) {
        viewModelScope.launch {
            val interaction = useCase.getInteractionById(id) ?: return@launch

            val appName =
                useCase.getAppName(interaction.packageName).valueOrNull() ?: interaction.packageName
            val appIcon = getAppIcon(interaction.packageName)

            val newState = SelectedUiElementState(
                description = "",
                appName = appName,
                appIcon = appIcon,
                nodeText = interaction.text ?: interaction.contentDescription,
                nodeClassName = interaction.className,
                nodeViewResourceId = interaction.viewResourceId,
                nodeUniqueId = interaction.uniqueId,
                interactionTypes = interaction.actions.toList(),
                selectedInteraction = interaction.userInteractedActionId
                    ?: interaction.actions.first(),
            )

            _selectedElementState.update { newState }
        }
    }

    fun onSelectInteractionTypeFilter(interactionType: NodeInteractionType?) {
        selectedInteractionTypeFilter.update { interactionType }
    }

    private suspend fun startRecording() {
        useCase.startRecording().onFailure { error ->
            if (error == Error.AccessibilityServiceDisabled) {
                ViewModelHelper.handleAccessibilityServiceStoppedDialog(
                    this,
                    this,
                    startService = { useCase.startService() },
                )
            } else if (error == Error.AccessibilityServiceCrashed) {
                ViewModelHelper.handleAccessibilityServiceCrashedDialog(
                    this,
                    this,
                    restartService = { useCase.startService() },
                )
            }
        }
    }

    private fun buildInteractedPackageListItem(packageName: String): SimpleListItemModel {
        val appName = useCase.getAppName(packageName).valueOrNull() ?: packageName
        val appIcon = getAppIcon(packageName)

        return SimpleListItemModel(
            id = packageName,
            title = appName,
            icon = appIcon,
        )
    }

    private fun buildUiElementListItem(node: AccessibilityNodeEntity): UiElementListItemModel {
        val resourceIdText = node.viewResourceId?.split("/")?.lastOrNull()

        return UiElementListItemModel(
            id = node.id,
            nodeViewResourceId = resourceIdText,
            nodeText = node.text ?: node.contentDescription,
            nodeClassName = node.className,
            nodeUniqueId = node.uniqueId?.toString(),
            interactionTypesText = node.actions.joinToString { getInteractionTypeString(it) },
            interactionTypes = node.actions,
        )
    }

    private fun buildInteractionTypeFilterItems(nodes: List<AccessibilityNodeEntity>): List<Pair<NodeInteractionType?, String>> {
        val interactionTypes = nodes.flatMap { it.actions }.toSet()

        return buildList {
            add(null to getString(R.string.action_interact_ui_element_interaction_type_any))

            // They should always be in the same order so iterate over the Enum entries.
            for (type in NodeInteractionType.entries) {
                if (interactionTypes.contains(type)) {
                    add(type to getInteractionTypeString(type))
                }
            }
        }
    }

    private fun getAppIcon(packageName: String): ComposeIconInfo = useCase
        .getAppIcon(packageName)
        .then { Success(ComposeIconInfo.Drawable(it)) }
        .otherwise { Success(ComposeIconInfo.Vector(Icons.Rounded.Android)) }
        .valueOrNull()!!

    private fun getInteractionTypeString(interactionType: NodeInteractionType): String {
        return when (interactionType) {
            NodeInteractionType.CLICK -> getString(R.string.action_interact_ui_element_interaction_type_click)
            NodeInteractionType.LONG_CLICK -> getString(R.string.action_interact_ui_element_interaction_type_long_click)
            NodeInteractionType.FOCUS -> getString(R.string.action_interact_ui_element_interaction_type_focus)
            NodeInteractionType.SELECT -> getString(R.string.action_interact_ui_element_interaction_type_select)
            NodeInteractionType.SCROLL_FORWARD -> getString(R.string.action_interact_ui_element_interaction_type_scroll_forward)
            NodeInteractionType.SCROLL_BACKWARD -> getString(R.string.action_interact_ui_element_interaction_type_scroll_backward)
            NodeInteractionType.EXPAND -> getString(R.string.action_interact_ui_element_interaction_type_expand)
            NodeInteractionType.COLLAPSE -> getString(R.string.action_interact_ui_element_interaction_type_collapse)
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val useCase: InteractUiElementUseCase,
        private val resourceProvider: ResourceProvider,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InteractUiElementViewModel(useCase, resourceProvider) as T
        }
    }
}

data class SelectedUiElementState(
    val description: String,
    val appName: String,
    val appIcon: ComposeIconInfo,
    val nodeText: String?,
    val nodeClassName: String?,
    val nodeViewResourceId: String?,
    val nodeUniqueId: String?,
    val interactionTypes: List<NodeInteractionType>,
    val selectedInteraction: NodeInteractionType,
)

sealed class RecordUiElementState {
    data class Recorded(val interactionCount: Int) : RecordUiElementState()

    data class CountingDown(
        val timeRemaining: String,
        val interactionCount: Int,
    ) : RecordUiElementState()

    data object Empty : RecordUiElementState()
}

data class SelectUiElementState(
    val listItems: List<UiElementListItemModel>,
    val interactionTypes: List<Pair<NodeInteractionType?, String>>,
    val selectedInteractionType: NodeInteractionType?,
)

data class UiElementListItemModel(
    val id: Long,
    val nodeViewResourceId: String?,
    val nodeText: String?,
    val nodeClassName: String?,
    val nodeUniqueId: String?,
    val interactionTypesText: String,
    val interactionTypes: Set<NodeInteractionType>,
)
