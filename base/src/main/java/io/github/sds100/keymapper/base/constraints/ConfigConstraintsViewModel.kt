package io.github.sds100.keymapper.base.constraints

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.ShortcutModel
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.isFixable
import io.github.sds100.keymapper.base.utils.ui.NavDestination
import io.github.sds100.keymapper.base.utils.ui.NavigationViewModel
import io.github.sds100.keymapper.base.utils.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.base.utils.ui.PopupViewModel
import io.github.sds100.keymapper.base.utils.ui.PopupViewModelImpl
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ViewModelHelper
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.navigate
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
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
import javax.inject.Inject

class ConfigConstraintsViewModel @Inject constructor(
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

    var showDuplicateConstraintsSnackbar: Boolean by mutableStateOf(false)

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

            if (error == SystemError.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY)) {
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
                showDuplicateConstraintsSnackbar = true
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

            io.github.sds100.keymapper.mapping.constraints.ConstraintListItemModel(
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
        val constraintList: List<io.github.sds100.keymapper.mapping.constraints.ConstraintListItemModel>,
        val selectedMode: ConstraintMode,
        val shortcuts: Set<ShortcutModel<Constraint>> = emptySet(),
    ) : ConfigConstraintsState()
}
