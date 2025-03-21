package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.isFixable
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.TintType
import io.github.sds100.keymapper.util.ui.ViewModelHelper
import io.github.sds100.keymapper.util.ui.navigate
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 29/11/20.
 */

class ConfigConstraintsViewModel(
    private val coroutineScope: CoroutineScope,
    private val displayUseCase: DisplayConstraintUseCase,
    private val configMappingUseCase: ConfigKeyMapUseCase,
    private val allowedConstraints: List<ConstraintId>,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    private val uiHelper = ConstraintUiHelperOld(displayUseCase, resourceProvider)

    private val _state by lazy { MutableStateFlow(buildState(State.Loading)) }
    val state by lazy { _state.asStateFlow() }

    init {
        val rebuildUiState = MutableSharedFlow<State<KeyMap>>()

        coroutineScope.launch {
            rebuildUiState.collectLatest { mapping ->
                _state.value = buildState(mapping.mapData { it.constraintState })
            }
        }

        coroutineScope.launch {
            configMappingUseCase.keyMap.collectLatest {
                rebuildUiState.emit(it)
            }
        }

        coroutineScope.launch {
            displayUseCase.invalidateConstraintErrors.collectLatest {
                rebuildUiState.emit(
                    configMappingUseCase.keyMap.firstOrNull() ?: return@collectLatest,
                )
            }
        }
    }

    fun onChosenNewConstraint(constraint: Constraint) {
        val isDuplicate = !configMappingUseCase.addConstraint(constraint)

        if (isDuplicate) {
            coroutineScope.launch {
                val snackBar = PopupUi.SnackBar(
                    message = getString(R.string.error_duplicate_constraint),
                )

                showPopup("duplicate_constraint", snackBar)
            }
        }
    }

    fun onRemoveConstraintClick(id: String) = configMappingUseCase.removeConstraint(id)

    fun onAndRadioButtonCheckedChange(checked: Boolean) {
        if (checked) {
            configMappingUseCase.setAndMode()
        }
    }

    fun onOrRadioButtonCheckedChange(checked: Boolean) {
        if (checked) {
            configMappingUseCase.setOrMode()
        }
    }

    fun onListItemClick(id: String) {
        coroutineScope.launch {
            configMappingUseCase.keyMap.firstOrNull()?.ifIsData { mapping ->
                val constraint = mapping.constraintState.constraints.singleOrNull { it.uid == id }
                    ?: return@launch

                val error = displayUseCase.getConstraintError(constraint) ?: return@launch

                if (error.isFixable) {
                    onFixError(error)
                }
            }
        }
    }

    private fun onFixError(error: Error) {
        coroutineScope.launch {
            if (error == Error.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY)) {
                coroutineScope.launch {
                    ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                        resourceProvider = this@ConfigConstraintsViewModel,
                        popupViewModel = this@ConfigConstraintsViewModel,
                        neverShowDndTriggerErrorAgain = { displayUseCase.neverShowDndTriggerError() },
                        fixError = { displayUseCase.fixError(error) },
                    )
                }
            } else {
                ViewModelHelper.showFixErrorDialog(
                    resourceProvider = this@ConfigConstraintsViewModel,
                    popupViewModel = this@ConfigConstraintsViewModel,
                    error,
                ) {
                    displayUseCase.fixError(error)
                }
            }
        }
    }

    fun onAddConstraintClick() {
        coroutineScope.launch {
            val constraint =
                navigate("add_constraint", NavDestination.ChooseConstraint(allowedConstraints))
                    ?: return@launch

            configMappingUseCase.addConstraint(constraint)
        }
    }

    private fun createListItem(constraint: Constraint): ConstraintListItem {
        val title: String = uiHelper.getTitle(constraint)
        val icon: IconInfo? = uiHelper.getIcon(constraint)
        val error: Error? = displayUseCase.getConstraintError(constraint)

        return ConstraintListItem(
            id = constraint.uid,
            tintType = icon?.tintType ?: TintType.Error,
            icon = icon?.drawable ?: getDrawable(R.drawable.ic_baseline_error_outline_24),
            title = title,
            errorMessage = error?.getFullMessage(this),
        )
    }

    private fun buildState(state: State<ConstraintState>): ConstraintListViewState = when (state) {
        is State.Data ->
            ConstraintListViewState(
                constraintList = State.Data(state.data.constraints.map { createListItem(it) }),
                showModeRadioButtons = state.data.constraints.size > 1,
                isAndModeChecked = state.data.mode == ConstraintMode.AND,
                isOrModeChecked = state.data.mode == ConstraintMode.OR,
            )

        is State.Loading ->
            ConstraintListViewState(
                constraintList = State.Loading,
                showModeRadioButtons = false,
                isAndModeChecked = false,
                isOrModeChecked = false,
            )
    }
}

data class ConstraintListViewState(
    val constraintList: State<List<ConstraintListItem>>,
    val showModeRadioButtons: Boolean,
    val isAndModeChecked: Boolean,
    val isOrModeChecked: Boolean,
)
