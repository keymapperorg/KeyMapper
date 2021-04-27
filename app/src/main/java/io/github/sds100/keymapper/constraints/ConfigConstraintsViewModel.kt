package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.ConfigMappingUseCase
import io.github.sds100.keymapper.mappings.DisplayConstraintUseCase
import io.github.sds100.keymapper.mappings.Mapping
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 29/11/20.
 */

class ConfigConstraintsViewModel(
    private val coroutineScope: CoroutineScope,
    private val display: DisplayConstraintUseCase,
    private val config: ConfigMappingUseCase<*, *>,
    val allowedConstraints: Array<ChooseConstraintType>,
    resourceProvider: ResourceProvider
) : ResourceProvider by resourceProvider, PopupViewModel by PopupViewModelImpl() {

    private val uiHelper = ConstraintUiHelper(display, resourceProvider)

    private val _state = MutableStateFlow(buildState(State.Loading))
    val state = _state.asStateFlow()

    init {
        val rebuildUiState = MutableSharedFlow<State<Mapping<*>>>()

        coroutineScope.launch {
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
            display.invalidateErrors.collectLatest {
                rebuildUiState.emit(config.mapping.firstOrNull() ?: return@collectLatest)
            }
        }
    }

    fun onChosenNewConstraint(constraint: Constraint) {
        val isDuplicate = !config.addConstraint(constraint)

        if (isDuplicate) {
            coroutineScope.launch {
                val snackBar = PopupUi.SnackBar(
                    message = getString(R.string.error_duplicate_constraint)
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

                val error = display.getConstraintError(constraint)?:return@launch

                if (error.isFixable) {
                   display.fixError(error)
                }
            }
        }
    }

    private fun createListItem(constraint: Constraint): ConstraintListItem {
        val title: String = uiHelper.getTitle(constraint)
        val icon: IconInfo? = uiHelper.getIcon(constraint)
        val error: Error? = display.getConstraintError(constraint)

        return ConstraintListItem(
            id = constraint.uid,
            tintType = icon?.tintType ?: TintType.ERROR,
            icon = icon?.drawable ?: getDrawable(R.drawable.ic_baseline_error_outline_24),
            title = title,
            errorMessage = error?.getFullMessage(this)
        )
    }

    private fun buildState(state: State<ConstraintState>): ConstraintListViewState {
        return when (state) {
            is State.Data ->
                ConstraintListViewState(
                    constraintList = state.data.constraints.map { createListItem(it) }
                        .createListState(),
                    showModeRadioButtons = state.data.constraints.size > 1,
                    isAndModeChecked = state.data.mode == ConstraintMode.AND,
                    isOrModeChecked = state.data.mode == ConstraintMode.OR
                )

            is State.Loading ->
                ConstraintListViewState(
                    constraintList = ListUiState.Loading,
                    showModeRadioButtons = false,
                    isAndModeChecked = false,
                    isOrModeChecked = false
                )
        }
    }
}

data class ConstraintListViewState(
    val constraintList: ListUiState<ConstraintListItem>,
    val showModeRadioButtons: Boolean,
    val isAndModeChecked: Boolean,
    val isOrModeChecked: Boolean
)