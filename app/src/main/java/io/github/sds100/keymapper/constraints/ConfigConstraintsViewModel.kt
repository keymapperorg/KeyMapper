package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.ShortcutModel
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.isFixable
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.ViewModelHelper
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.util.ui.navigate
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 29/11/20.
 */

class ConfigConstraintsViewModel(
    private val coroutineScope: CoroutineScope,
    private val config: ConfigKeyMapUseCase,
    private val displayConstraint: DisplayConstraintUseCase,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    private val uiHelper = ConstraintUiHelper(displayConstraint, resourceProvider)

    private val _state: MutableStateFlow<State<ConfigConstraintsState>> =
        MutableStateFlow(State.Loading)
    val state = _state.asStateFlow()

    private val shortcuts: StateFlow<Set<ShortcutModel<Constraint>>> =
        config.recentlyUsedConstraints.map { actions ->
            actions.map(::buildShortcut).toSet()
        }.stateIn(coroutineScope, SharingStarted.Lazily, emptySet())

    private val constraintErrorSnapshot: StateFlow<ConstraintErrorSnapshot?> =
        displayConstraint.constraintErrorSnapshot.stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            null,
        )

    init {
        combine(
            config.keyMap,
            shortcuts,
            constraintErrorSnapshot.filterNotNull(),
        ) { keyMapState, shortcuts, errorSnapshot ->
            _state.value = keyMapState.mapData { keyMap ->
                buildState(keyMap.constraintState, shortcuts, errorSnapshot)
            }
        }.launchIn(coroutineScope)
    }

    fun onClickShortcut(constraint: Constraint) {
        coroutineScope.launch {
            config.addConstraint(constraint)
        }
    }

    fun onRemoveClick(id: String) = config.removeConstraint(id)

    fun onSelectMode(mode: ConstraintMode) {
        when (mode) {
            ConstraintMode.AND -> config.setAndMode()
            ConstraintMode.OR -> config.setOrMode()
        }
    }

    fun onFixError(constraintUid: String) {
        coroutineScope.launch {
            val constraint = config.keyMap
                .firstOrNull()
                ?.dataOrNull()
                ?.constraintState
                ?.constraints
                ?.find { it.uid == constraintUid }
                ?: return@launch

            val error = constraintErrorSnapshot.filterNotNull().first().getError(constraint)
                ?: return@launch

            if (error == Error.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY)) {
                coroutineScope.launch {
                    ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                        resourceProvider = this@ConfigConstraintsViewModel,
                        popupViewModel = this@ConfigConstraintsViewModel,
                        neverShowDndTriggerErrorAgain = { displayConstraint.neverShowDndTriggerError() },
                        fixError = { displayConstraint.fixError(error) },
                    )
                }
            } else {
                ViewModelHelper.showFixErrorDialog(
                    resourceProvider = this@ConfigConstraintsViewModel,
                    popupViewModel = this@ConfigConstraintsViewModel,
                    error,
                ) {
                    displayConstraint.fixError(error)
                }
            }
        }
    }

    fun addConstraint() {
        coroutineScope.launch {
            val constraint =
                navigate("add_constraint", NavDestination.ChooseConstraint)
                    ?: return@launch

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
    }

    private fun buildShortcut(constraint: Constraint): ShortcutModel<Constraint> {
        return ShortcutModel(
            icon = uiHelper.getIcon(constraint),
            text = uiHelper.getTitle(constraint),
            data = constraint,
        )
    }

    private fun buildState(
        state: ConstraintState,
        shortcuts: Set<ShortcutModel<Constraint>>,
        errorSnapshot: ConstraintErrorSnapshot,
    ): ConfigConstraintsState {
        if (state.constraints.isEmpty()) {
            return ConfigConstraintsState.Empty(shortcuts)
        }

        val constraintList = state.constraints.mapIndexed { index, constraint ->
            val title: String = uiHelper.getTitle(constraint)
            val icon: ComposeIconInfo = uiHelper.getIcon(constraint)
            val error: Error? = errorSnapshot.getError(constraint)

            ConstraintListItemModel(
                id = constraint.uid,
                icon = icon,
                constraintModeLink = if (state.constraints.size > 1 && index < state.constraints.size - 1) {
                    state.mode
                } else {
                    null
                },
                text = title,
                error = error?.getFullMessage(this),
                isErrorFixable = error?.isFixable ?: true,
            )
        }

        return ConfigConstraintsState.Loaded(
            constraintList = constraintList,
            selectedMode = state.mode,
            shortcuts = shortcuts,
        )
    }
}

sealed class ConfigConstraintsState {
    data class Empty(
        val shortcuts: Set<ShortcutModel<Constraint>> = emptySet(),
    ) : ConfigConstraintsState()

    data class Loaded(
        val constraintList: List<ConstraintListItemModel>,
        val selectedMode: ConstraintMode,
        val shortcuts: Set<ShortcutModel<Constraint>> = emptySet(),
    ) : ConfigConstraintsState()
}
