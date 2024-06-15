package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.ConfigMappingUseCase
import io.github.sds100.keymapper.mappings.DisplayConstraintUseCase
import io.github.sds100.keymapper.mappings.Mapping
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
import io.github.sds100.keymapper.util.ui.navigate
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val display: DisplayConstraintUseCase,
    private val config: ConfigMappingUseCase<*, *>,
    private val allowedConstraints: List<ChooseConstraintType>,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    private val uiHelper = ConstraintUiHelper(display, resourceProvider)

    private val _state by lazy { MutableStateFlow(buildState(State.Loading)) }
    val state by lazy { _state.asStateFlow() }

    init {
        val rebuildUiState = MutableSharedFlow<State<Mapping<*>>>()

        coroutineScope.launch(Dispatchers.Default) {
            rebuildUiState.collectLatest { mapping ->
                _state.value = buildState(mapping.mapData { it.constraintState })
            }
        }

        coroutineScope.launch {
            config.mapping.collectLatest {
                rebuildUiState.emit(it)
            }
        }

        coroutineScope.launch {
            display.invalidateConstraintErrors.collectLatest {
                rebuildUiState.emit(config.mapping.firstOrNull() ?: return@collectLatest)
            }
        }
    }

    fun onChosenNewConstraint(constraint: Constraint) {
        val isDuplicate = !config.addConstraint(constraint)

        if (isDuplicate) {
            coroutineScope.launch {
                val snackBar = PopupUi.SnackBar(
                    message = getString(R.string.error_duplicate_constraint),
                )

                showPopup("duplicate_constraint", snackBar)
            }
        }
    }

    fun onRemoveConstraintClick(id: String) = config.removeConstraint(id)

    fun onAndRadioButtonCheckedChange(checked: Boolean) {
        if (checked) {
            config.setAndMode()
        }
    }

    fun onOrRadioButtonCheckedChange(checked: Boolean) {
        if (checked) {
            config.setOrMode()
        }
    }

    fun onListItemClick(id: String) {
        coroutineScope.launch {
            config.mapping.firstOrNull()?.ifIsData { mapping ->
                val constraint = mapping.constraintState.constraints.singleOrNull { it.uid == id }
                    ?: return@launch

                val error = display.getConstraintError(constraint) ?: return@launch

                if (error.isFixable) {
                    display.fixError(error)
                }
            }
        }
    }

    fun onAddConstraintClick() {
        coroutineScope.launch {
            val constraint =
                navigate("add_constraint", NavDestination.ChooseConstraint(allowedConstraints))
                    ?: return@launch

            config.addConstraint(constraint)
        }
    }

    private fun createListItem(constraint: Constraint): ConstraintListItem {
        val title: String = uiHelper.getTitle(constraint)
        val icon: IconInfo? = uiHelper.getIcon(constraint)
        val error: Error? = display.getConstraintError(constraint)

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
