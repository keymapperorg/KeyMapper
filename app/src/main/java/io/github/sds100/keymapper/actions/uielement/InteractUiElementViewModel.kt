package io.github.sds100.keymapper.actions.uielement

import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ActionData
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
        .map { state -> state.mapData(::createInteractedPackageListItems) }

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

    fun loadAction(action: ActionData.InteractUiElement) {
        viewModelScope.launch {
            val appName = useCase.getAppName(action.packageName).valueOrNull() ?: action.packageName
            val appIcon = useCase.getAppIcon(action.packageName).valueOrNull()
                ?.let { ComposeIconInfo.Drawable(it) }

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

    private fun createInteractedPackageListItems(packages: List<String>): List<SimpleListItemModel> {
        return packages.map { packageName ->
            val appName = useCase.getAppName(packageName).valueOrNull() ?: packageName
            val appIcon = useCase
                .getAppIcon(packageName)
                .then { Success(ComposeIconInfo.Drawable(it)) }
                .otherwise { Success(ComposeIconInfo.Vector(Icons.Rounded.Android)) }
                .valueOrNull()!!

            SimpleListItemModel(
                id = packageName,
                title = appName,
                icon = appIcon,
            )
        }
    }

    private fun getNodeActionName(nodeAction: Int): String {
        return when (nodeAction) {
            AccessibilityNodeInfo.ACTION_CLICK -> getString(R.string.action_interact_ui_element_interaction_type_click)
            AccessibilityNodeInfo.ACTION_LONG_CLICK -> getString(R.string.action_interact_ui_element_interaction_type_long_click)
            AccessibilityNodeInfo.ACTION_FOCUS -> getString(R.string.action_interact_ui_element_interaction_type_focus)
            AccessibilityNodeInfo.ACTION_SELECT -> getString(R.string.action_interact_ui_element_interaction_type_select)
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> getString(R.string.action_interact_ui_element_interaction_type_scroll_forward)
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> getString(R.string.action_interact_ui_element_interaction_type_scroll_backward)
            AccessibilityNodeInfo.ACTION_EXPAND -> getString(R.string.action_interact_ui_element_interaction_type_expand)
            AccessibilityNodeInfo.ACTION_COLLAPSE -> getString(R.string.action_interact_ui_element_interaction_type_collapse)
            else -> getString(
                R.string.action_interact_ui_element_interaction_type_unknown,
                nodeAction,
            )
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
    val appIcon: ComposeIconInfo.Drawable?,
    val nodeText: String?,
    val nodeClassName: String?,
    val nodeViewResourceId: String?,
    val nodeUniqueId: String?,
    val interactionTypes: List<Int>,
    val selectedInteraction: Int,
)

sealed class RecordUiElementState {
    data class Recorded(val interactionCount: Int) : RecordUiElementState()

    data class CountingDown(
        val timeRemaining: String,
        val interactionCount: Int,
    ) : RecordUiElementState()

    data object Empty : RecordUiElementState()
}
