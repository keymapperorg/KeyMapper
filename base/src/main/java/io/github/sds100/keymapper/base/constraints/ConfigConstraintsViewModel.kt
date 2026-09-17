package io.github.sds100.keymapper.base.constraints

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.keymaps.ShortcutModel
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.isFixable
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ViewModelHelper
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import javax.inject.Inject
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

@HiltViewModel
class ConfigConstraintsViewModel @Inject constructor(
    private val config: ConfigConstraintsUseCase,
    private val displayConstraint: DisplayConstraintUseCase,
    resourceProvider: ResourceProvider,
    navigationProvider: NavigationProvider,
    dialogProvider: DialogProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    private val uiHelper = ConstraintUiHelper(displayConstraint, resourceProvider)

    private val _state: MutableStateFlow<State<ConfigConstraintsState>> =
        MutableStateFlow(State.Loading)
    val state = _state.asStateFlow()

    private val shortcuts: StateFlow<Set<ShortcutModel<ConstraintData>>> =
        config.recentlyUsedConstraints.map { constraintDataList ->
            constraintDataList.map { constraintData ->
                buildShortcutFromData(constraintData)
            }.toSet()
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    private val constraintErrorSnapshot: StateFlow<ConstraintErrorSnapshot?> =
        displayConstraint.constraintErrorSnapshot.stateIn(
            viewModelScope,
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
        }.launchIn(viewModelScope)
    }

    fun onClickShortcut(constraintData: ConstraintData) {
        viewModelScope.launch {
            config.addConstraint(groupUid = null, constraintData)
        }
    }

    fun onRemoveClick(id: String) = config.removeConstraint(id)

    fun onRemoveGroupClick(groupUid: String) = config.removeGroup(groupUid)

    fun onNotClick(constraintUid: String) = config.toggleNot(constraintUid)

    fun onSelectMode(mode: ConstraintMode) = config.setMode(mode)

    fun onSelectGroupMode(groupUid: String, mode: ConstraintMode) {
        config.setGroupMode(groupUid, mode)
    }

    fun onRenameGroup(groupUid: String, name: String) {
        config.setGroupName(groupUid, name)
    }

    fun onMoveGroup(fromIndex: Int, toIndex: Int) = config.moveGroup(fromIndex, toIndex)

    fun onMoveConstraint(groupUid: String, fromIndex: Int, toIndex: Int) {
        config.moveConstraint(groupUid, fromIndex, toIndex)
    }

    fun onFixError(constraintUid: String) {
        viewModelScope.launch {
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
                viewModelScope.launch {
                    ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                        resourceProvider = this@ConfigConstraintsViewModel,
                        dialogProvider = this@ConfigConstraintsViewModel,
                        neverShowDndTriggerErrorAgain = {
                            displayConstraint.neverShowDndTriggerError()
                        },
                        fixError = { displayConstraint.fixError(error) },
                    )
                }
            } else {
                ViewModelHelper.showFixErrorDialog(
                    resourceProvider = this@ConfigConstraintsViewModel,
                    dialogProvider = this@ConfigConstraintsViewModel,
                    error,
                ) {
                    displayConstraint.fixError(error)
                }
            }
        }
    }

    /**
     * @param groupUid the group to add the constraint to. A new group is created if this is null.
     */
    fun addConstraint(groupUid: String?) {
        viewModelScope.launch {
            val constraint: ConstraintData =
                navigate("add_constraint", NavDestination.ChooseConstraint)
                    ?: return@launch

            val isDuplicate = !config.addConstraint(groupUid, constraint)

            if (isDuplicate) {
                showDuplicateConstraintsSnackbar = true
            }
        }
    }

    private fun buildShortcutFromData(
        constraintData: ConstraintData,
    ): ShortcutModel<ConstraintData> {
        val constraint = Constraint(data = constraintData)
        return ShortcutModel(
            icon = uiHelper.getIcon(constraint),
            text = uiHelper.getTitle(constraint),
            data = constraintData,
        )
    }

    private fun buildState(
        state: ConstraintState,
        shortcuts: Set<ShortcutModel<ConstraintData>>,
        errorSnapshot: ConstraintErrorSnapshot,
    ): ConfigConstraintsState {
        if (state.constraints.isEmpty()) {
            return ConfigConstraintsState.Empty(shortcuts)
        }

        val groups = state.groups
            .filter { it.constraints.isNotEmpty() }
            .map { group ->
                val constraints = group.constraints.map { constraint ->
                    val error: KMError? = errorSnapshot.getError(constraint)

                    ConstraintListItemModel(
                        id = constraint.uid,
                        icon = uiHelper.getIcon(constraint),
                        text = uiHelper.getTitle(constraint),
                        isNot = constraint.isNot,
                        error = error?.getFullMessage(this),
                        isErrorFixable = error?.isFixable ?: true,
                    )
                }

                val modeWord = when (group.mode) {
                    ConstraintMode.AND -> getString(R.string.constraint_mode_and)
                    ConstraintMode.OR -> getString(R.string.constraint_mode_or)
                }

                ConstraintGroupListItemModel(
                    uid = group.uid,
                    name = group.name,
                    mode = group.mode,
                    constraints = constraints,
                    description = group.constraints.joinToString(separator = " $modeWord ") {
                        uiHelper.getTitle(it)
                    },
                )
            }

        return ConfigConstraintsState.Loaded(
            groups = groups,
            mode = state.mode,
            shortcuts = shortcuts,
        )
    }
}

sealed class ConfigConstraintsState {
    data class Empty(val shortcuts: Set<ShortcutModel<ConstraintData>> = emptySet()) :
        ConfigConstraintsState()

    data class Loaded(
        val groups: List<ConstraintGroupListItemModel>,
        /**
         * The mode that combines the groups.
         */
        val mode: ConstraintMode,
        val shortcuts: Set<ShortcutModel<ConstraintData>> = emptySet(),
    ) : ConfigConstraintsState()
}
