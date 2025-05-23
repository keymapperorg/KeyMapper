package io.github.sds100.keymapper.base.actions.uielement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.system.accessibility.RecordAccessibilityNodeState
import io.github.sds100.keymapper.base.utils.containsQuery
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.PopupViewModel
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ViewModelHelper
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.SimpleListItemModel
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.NodeInteractionType
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.ifIsData
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class InteractUiElementViewModel @Inject constructor(
    private val useCase: InteractUiElementUseCase,
    resourceProvider: ResourceProvider,
    popupViewModel: PopupViewModel,
    navigationProvider: NavigationProvider,
) : ViewModel(),
    NavigationProvider by navigationProvider,
    PopupViewModel by popupViewModel,
    ResourceProvider by resourceProvider {

    val recordState: StateFlow<State<RecordUiElementState>> = combine(
        useCase.recordState,
        useCase.interactionCount,
    ) { recordState, interactionCountState ->
        val interactionCount = interactionCountState.dataOrNull() ?: return@combine State.Loading

        when (recordState) {
            is RecordAccessibilityNodeState.CountingDown -> {
                val mins = recordState.timeLeft / 60
                val secs = recordState.timeLeft % 60

                val timeRemainingText = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    mins,
                    secs,
                )

                State.Data(
                    RecordUiElementState.CountingDown(
                        timeRemaining = timeRemainingText,
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

    private val selectedElementEntity = MutableStateFlow<AccessibilityNodeEntity?>(null)
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
        .onEach { state ->
            // Automatically show additional elements if no elements that were interacted with
            // were detected.
            state.ifIsData { list ->
                if (list.count { it.interacted } == 0) {
                    showAdditionalElements.update { true }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, State.Loading)

    private val elementListItems: Flow<State<List<UiElementListItemModel>>> = interactionsByPackage
        .map { state -> state.mapData { list -> list.map(::buildUiElementListItem) } }

    private val interactionTypesFilterItems: Flow<State<List<Pair<NodeInteractionType?, String>>>> =
        interactionsByPackage
            .map { state ->
                state.mapData { list ->
                    val any = Pair(
                        null,
                        getString(R.string.action_interact_ui_element_interaction_type_any),
                    )

                    val interactionTypes = list.flatMap { it.actions }.toSet()

                    listOf(any).plus(buildInteractionTypeFilterItems(interactionTypes))
                }
            }

    private val selectedInteractionTypeFilter = MutableStateFlow<NodeInteractionType?>(null)

    private val showAdditionalElements: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val filteredElementListItems = combine(
        elementListItems,
        elementSearchQuery,
        selectedInteractionTypeFilter,
        showAdditionalElements,
    ) { state, query, interactionType, showAdditionalElements ->
        state.mapData { listItems ->
            listItems.filter { model ->
                if (!showAdditionalElements && !model.interacted) {
                    return@filter false
                }

                if (interactionType != null && !model.interactionTypes.contains(interactionType)) {
                    return@filter false
                }

                val modelString = buildString {
                    append(model.nodeText)
                    append(" ")
                    append(model.nodeTooltipHint)
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
        showAdditionalElements,
    ) { listItemsState, interactionTypesState, selectedInteractionType, showAdditionalElements ->
        val listItems = listItemsState.dataOrNull() ?: return@combine State.Loading
        val interactionTypes = interactionTypesState.dataOrNull() ?: return@combine State.Loading

        val newState = SelectUiElementState(
            listItems = listItems,
            interactionTypes = interactionTypes,
            selectedInteractionType = selectedInteractionType,
            showAdditionalElements = showAdditionalElements,
        )
        State.Data(newState)
    }.stateIn(viewModelScope, SharingStarted.Lazily, State.Loading)

    fun loadAction(action: ActionData.InteractUiElement) {
        viewModelScope.launch {
            val appName = useCase.getAppName(action.packageName).valueOrNull() ?: action.packageName
            val appIcon = getAppIcon(action.packageName)

            val newState = SelectedUiElementState(
                description = action.description,
                packageName = action.packageName,
                appName = appName,
                appIcon = appIcon,
                nodeText = action.text ?: action.contentDescription,
                nodeToolTipHint = action.tooltip ?: action.hint,
                nodeClassName = action.className,
                nodeViewResourceId = action.viewResourceId,
                nodeUniqueId = action.uniqueId,
                interactionTypes = buildInteractionTypeFilterItems(action.nodeActions),
                selectedInteraction = action.nodeAction,
            )

            _selectedElementState.update { newState }
        }
    }

    fun onDoneClick() {
        val selectedElementState = _selectedElementState.value
        val selectedElementEntity = selectedElementEntity.value

        if (selectedElementState == null || selectedElementEntity == null) {
            return
        }

        if (selectedElementState.description.isBlank()) {
            return
        }

        val action = ActionData.InteractUiElement(
            description = selectedElementState.description,
            nodeAction = selectedElementState.selectedInteraction,
            packageName = selectedElementEntity.packageName,
            text = selectedElementEntity.text,
            contentDescription = selectedElementEntity.contentDescription,
            tooltip = selectedElementEntity.tooltip,
            hint = selectedElementEntity.hint,
            className = selectedElementEntity.className,
            viewResourceId = selectedElementEntity.viewResourceId,
            uniqueId = selectedElementEntity.uniqueId,
            nodeActions = selectedElementEntity.actions,
        )

        viewModelScope.launch {
            popBackStackWithResult(Json.encodeToString(action))
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            popBackStack()
        }
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

        if (packageName != selectedApp.value) {
            showAdditionalElements.update { false }
        }

        selectedApp.update { packageName }
    }

    fun onSelectElement(id: Long) {
        viewModelScope.launch {
            val interaction = useCase.getInteractionById(id) ?: return@launch

            val appName =
                useCase.getAppName(interaction.packageName).valueOrNull() ?: interaction.packageName
            val appIcon = getAppIcon(interaction.packageName)

            val selectedInteraction =
                NodeInteractionType.entries.first { interaction.actions.contains(it) }
            val interactionText = getInteractionTypeString(selectedInteraction)
            val descriptionElement =
                interaction.text ?: interaction.contentDescription ?: interaction.tooltip
                    ?: interaction.hint ?: interaction.viewResourceId

            val description = if (descriptionElement == null) {
                ""
            } else {
                "$interactionText: $descriptionElement"
            }

            val newState = SelectedUiElementState(
                description = description,
                packageName = interaction.packageName,
                appName = appName,
                appIcon = appIcon,
                nodeText = interaction.text ?: interaction.contentDescription,
                nodeClassName = interaction.className,
                nodeToolTipHint = interaction.tooltip ?: interaction.hint,
                nodeViewResourceId = interaction.viewResourceId,
                nodeUniqueId = interaction.uniqueId,
                interactionTypes = buildInteractionTypeFilterItems(interaction.actions),
                selectedInteraction = selectedInteraction,
            )

            selectedElementEntity.update { interaction }
            _selectedElementState.update { newState }
        }
    }

    fun onSelectElementInteractionType(interactionType: NodeInteractionType) {
        _selectedElementState.update { state ->
            state?.copy(selectedInteraction = interactionType)
        }
    }

    fun onSelectInteractionTypeFilter(interactionType: NodeInteractionType?) {
        selectedInteractionTypeFilter.update { interactionType }
    }

    fun onDescriptionChanged(description: String) {
        _selectedElementState.update { state ->
            state?.copy(description = description)
        }
    }

    fun onAdditionalElementsCheckedChanged(checked: Boolean) {
        showAdditionalElements.update { checked }
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
        val appIcon = getAppIcon(packageName) ?: ComposeIconInfo.Vector(Icons.Rounded.Android)

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
            nodeUniqueId = node.uniqueId,
            nodeTooltipHint = node.tooltip ?: node.hint,
            interactionTypesText = node.actions.joinToString { getInteractionTypeString(it) },
            interactionTypes = node.actions,
            interacted = node.interacted,
        )
    }

    private fun buildInteractionTypeFilterItems(interactionTypes: Set<NodeInteractionType>): List<Pair<NodeInteractionType, String>> {
        return buildList {
            // They should always be in the same order so iterate over the Enum entries.
            for (type in NodeInteractionType.entries) {
                if (interactionTypes.contains(type)) {
                    add(type to getInteractionTypeString(type))
                }
            }
        }
    }

    private fun getAppIcon(packageName: String): ComposeIconInfo.Drawable? = useCase
        .getAppIcon(packageName)
        .then { Success(ComposeIconInfo.Drawable(it)) }
        .valueOrNull()

    private fun getInteractionTypeString(interactionType: NodeInteractionType): String {
        return when (interactionType) {
            NodeInteractionType.CLICK -> getString(R.string.action_interact_ui_element_interaction_type_click)
            NodeInteractionType.LONG_CLICK -> getString(R.string.action_interact_ui_element_interaction_type_long_click)
            NodeInteractionType.FOCUS -> getString(R.string.action_interact_ui_element_interaction_type_focus)
            NodeInteractionType.SCROLL_FORWARD -> getString(R.string.action_interact_ui_element_interaction_type_scroll_forward)
            NodeInteractionType.SCROLL_BACKWARD -> getString(R.string.action_interact_ui_element_interaction_type_scroll_backward)
            NodeInteractionType.EXPAND -> getString(R.string.action_interact_ui_element_interaction_type_expand)
            NodeInteractionType.COLLAPSE -> getString(R.string.action_interact_ui_element_interaction_type_collapse)
        }
    }
}

data class SelectedUiElementState(
    val description: String,
    val packageName: String,
    val appName: String,
    val appIcon: ComposeIconInfo.Drawable?,
    val nodeText: String?,
    val nodeToolTipHint: String?,
    val nodeClassName: String?,
    val nodeViewResourceId: String?,
    val nodeUniqueId: String?,
    val interactionTypes: List<Pair<NodeInteractionType, String>>,
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
    val showAdditionalElements: Boolean,
)

data class UiElementListItemModel(
    val id: Long,
    val nodeViewResourceId: String?,
    val nodeText: String?,
    val nodeTooltipHint: String?,
    val nodeClassName: String?,
    val nodeUniqueId: String?,
    val interactionTypesText: String,
    val interactionTypes: Set<NodeInteractionType>,
    /**
     * Whether the user interacted with this element.
     */
    val interacted: Boolean,
)
