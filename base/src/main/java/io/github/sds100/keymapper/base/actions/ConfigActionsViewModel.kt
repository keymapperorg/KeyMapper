package io.github.sds100.keymapper.base.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.keyevent.FixKeyEventActionDelegate
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.keymaps.ShortcutModel
import io.github.sds100.keymapper.base.onboarding.OnboardingTapTarget
import io.github.sds100.keymapper.base.onboarding.OnboardingTipDelegate
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.onboarding.SetupAccessibilityServiceDelegate
import io.github.sds100.keymapper.base.trigger.EvdevTriggerKey
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.isFixable
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ViewModelHelper
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.common.utils.AccessibilityServiceError
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.common.utils.onFailure
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ConfigActionsViewModel @Inject constructor(
    private val displayAction: DisplayActionUseCase,
    private val createAction: CreateActionUseCase,
    private val testAction: TestActionUseCase,
    private val config: ConfigActionsUseCase,
    private val onboardingUseCase: OnboardingUseCase,
    setupAccessibilityServiceDelegate: SetupAccessibilityServiceDelegate,
    fixKeyEventActionDelegate: FixKeyEventActionDelegate,
    private val onboardingTipDelegate: OnboardingTipDelegate,
    resourceProvider: ResourceProvider,
    navigationProvider: NavigationProvider,
    dialogProvider: DialogProvider,
) : ViewModel(),
    ActionOptionsBottomSheetCallback,
    ActionListCallback,
    SetupAccessibilityServiceDelegate by setupAccessibilityServiceDelegate,
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider,
    FixKeyEventActionDelegate by fixKeyEventActionDelegate,
    OnboardingTipDelegate by onboardingTipDelegate {

    val createActionDelegate =
        CreateActionDelegate(viewModelScope, createAction, this, this, this)
    private val uiHelper = ActionUiHelper(displayAction, resourceProvider)

    private val _state = MutableStateFlow<State<ConfigActionsState>>(State.Loading)
    val state = _state.asStateFlow()

    private val shortcuts: StateFlow<Set<ShortcutModel<ActionData>>> =
        config.recentlyUsedActions.map { actions ->
            actions.map(::buildShortcut).toSet()
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val actionOptionsUid = MutableStateFlow<String?>(null)
    val actionOptionsState: StateFlow<ActionOptionsState?> =
        combine(config.keyMap, actionOptionsUid, transform = ::buildOptionsState)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private var editedActionUid: String? = null

    private val actionErrorSnapshot: StateFlow<ActionErrorSnapshot?> =
        displayAction.actionErrorSnapshot.stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        combine(
            config.keyMap,
            shortcuts,
            displayAction.showDeviceDescriptors,
            actionErrorSnapshot.filterNotNull(),
        ) { keyMapState, shortcuts, showDeviceDescriptors, errorSnapshot ->
            _state.value = keyMapState.mapData { keyMap ->
                buildState(keyMap, shortcuts, errorSnapshot, showDeviceDescriptors)
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            createActionDelegate.actionResult.filterNotNull().collect { action ->
                val actionUid = editedActionUid ?: return@collect
                config.setActionData(actionUid, action)
                actionOptionsUid.update { null }
            }
        }
    }

    private suspend fun getActionData(uid: String): ActionData? {
        return config.keyMap.first().dataOrNull()?.actionList?.singleOrNull { it.uid == uid }?.data
    }

    override fun onClickShortcut(action: ActionData) {
        viewModelScope.launch {
            config.addAction(action)
        }
    }

    override fun onFixErrorClick(actionUid: String) {
        viewModelScope.launch {
            val actionData = getActionData(actionUid) ?: return@launch
            val error =
                actionErrorSnapshot.filterNotNull().first().getError(actionData) ?: return@launch

            when (error) {
                SystemError.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY) -> {
                    viewModelScope.launch {
                        ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                            resourceProvider = this@ConfigActionsViewModel,
                            dialogProvider = this@ConfigActionsViewModel,
                            neverShowDndTriggerErrorAgain = {
                                displayAction.neverShowDndTriggerError()
                            },
                            fixError = { displayAction.fixError(error) },
                        )
                    }
                }

                is KMError.KeyEventActionError -> {
                    showFixKeyEventActionBottomSheet()
                }

                else -> {
                    ViewModelHelper.showFixErrorDialog(
                        resourceProvider = this@ConfigActionsViewModel,
                        dialogProvider = this@ConfigActionsViewModel,
                        error,
                    ) {
                        displayAction.fixError(error)
                    }
                }
            }
        }
    }

    fun onAddActionClick() {
        viewModelScope.launch {
            val actionData = navigate("add_action", NavDestination.ChooseAction) ?: return@launch

            config.addAction(actionData)

            // Never show the tap target to add an action again.
            onboardingUseCase.completedTapTarget(OnboardingTapTarget.CHOOSE_ACTION)
        }
    }

    override fun onMove(fromIndex: Int, toIndex: Int) {
        config.moveAction(fromIndex, toIndex)
    }

    fun onRemoveClick(actionUid: String) {
        config.removeAction(actionUid)
    }

    override fun onEditClick(actionUid: String) {
        actionOptionsUid.value = actionUid
    }

    override fun onTestClick(actionUid: String) {
        viewModelScope.launch {
            val actionData = getActionData(actionUid) ?: return@launch
            attemptTestAction(actionData)
        }
    }

    override fun onDuplicateClick(actionUid: String) {
        config.duplicateAction(actionUid)
    }

    override fun onEditClick() {
        val actionUid = actionOptionsUid.value ?: return
        viewModelScope.launch {
            // Clear the bottom sheet so navigating back with predicted-back works
            actionOptionsUid.update { null }
            editedActionUid = actionUid

            val keyMap = config.keyMap.first().dataOrNull() ?: return@launch

            val oldAction = keyMap.actionList.find { it.uid == actionUid } ?: return@launch
            createActionDelegate.editAction(oldAction.data)
        }
    }

    override fun onReplaceClick() {
        val actionUid = actionOptionsUid.value ?: return
        viewModelScope.launch {
            // Clear the bottom sheet so navigating back with predicted-back works
            actionOptionsUid.update { null }

            val newActionData =
                navigate("replace_action", NavDestination.ChooseAction) ?: return@launch

            config.setActionData(actionUid, newActionData)
        }
    }

    override fun onRepeatLimitChanged(limit: Int) {
        actionOptionsUid.value?.let { uid -> config.setActionRepeatLimit(uid, limit) }
    }

    override fun onRepeatRateChanged(rate: Int) {
        actionOptionsUid.value?.let { uid -> config.setActionRepeatRate(uid, rate) }
    }

    override fun onRepeatDelayChanged(delay: Int) {
        actionOptionsUid.value?.let { uid -> config.setActionRepeatDelay(uid, delay) }
    }

    override fun onHoldDownDurationChanged(duration: Int) {
        actionOptionsUid.value?.let { uid -> config.setActionHoldDownDuration(uid, duration) }
    }

    override fun onSelectHoldDownMode(holdDownMode: HoldDownMode?) {
        val uid = actionOptionsUid.value ?: return

        if (holdDownMode == null) {
            config.setActionHoldDownEnabled(uid, false)
            return
        }

        config.setActionHoldDownEnabled(uid, true)
        config.setActionStopHoldingDownWhenTriggerPressedAgain(
            uid,
            holdDownMode == HoldDownMode.TRIGGER_PRESSED_AGAIN,
        )
    }

    fun onDelayChanged(actionUid: String, delay: Int) {
        config.setDelayBeforeNextAction(actionUid, delay)
    }

    override fun onEnabledChange(actionUid: String, enabled: Boolean) {
        config.setActionEnabled(actionUid, enabled)
    }

    override fun onActionTipDismiss() {
        onActionTipDismissClick()
    }

    override fun onTipButtonClick(id: String) {
        onboardingTipDelegate.onTipButtonClick(id)
    }

    override fun onMultiplierChanged(multiplier: Int) {
        actionOptionsUid.value?.let { uid -> config.setActionMultiplier(uid, multiplier) }
    }

    override fun onSelectRepeatMode(repeatMode: RepeatMode?) {
        val uid = actionOptionsUid.value ?: return

        if (repeatMode == null) {
            config.setActionRepeatEnabled(uid, false)
            return
        }

        config.setActionRepeatEnabled(uid, true)

        when (repeatMode) {
            RepeatMode.TRIGGER_RELEASED -> config.setActionStopRepeatingWhenTriggerReleased(uid)

            RepeatMode.LIMIT_REACHED -> config.setActionStopRepeatingWhenLimitReached(uid)

            RepeatMode.TRIGGER_PRESSED_AGAIN ->
                config.setActionStopRepeatingWhenTriggerPressedAgain(uid)
        }
    }

    override fun onCustomNameChanged(name: String) {
        val uid = actionOptionsUid.value ?: return
        onRenameAction(uid, name)
    }

    fun onRenameAction(uid: String, name: String) {
        viewModelScope.launch {
            val actionData = getActionData(uid) ?: return@launch
            val showDeviceDescriptors = displayAction.showDeviceDescriptors.first()
            val generatedTitle = uiHelper.getTitle(actionData, showDeviceDescriptors)

            // Submitting the generated title removes the custom name.
            val customName = name.trim().takeIf { it.isNotEmpty() && it != generatedTitle }
            config.setActionCustomName(uid, customName)
        }
    }

    private suspend fun attemptTestAction(actionData: ActionData) {
        testAction.invoke(actionData).onFailure { error ->
            if (error is AccessibilityServiceError) {
                showFixAccessibilityServiceDialog(error)
            }
        }
    }

    private fun buildShortcut(action: ActionData): ShortcutModel<ActionData> {
        return ShortcutModel(
            icon = uiHelper.getIcon(action),
            text = uiHelper.getTitle(action, false),
            data = action,
        )
    }

    private fun buildState(
        keyMap: KeyMap,
        shortcuts: Set<ShortcutModel<ActionData>>,
        errorSnapshot: ActionErrorSnapshot,
        showDeviceDescriptors: Boolean,
    ): ConfigActionsState {
        if (keyMap.actionList.isEmpty()) {
            return ConfigActionsState.Empty(shortcuts = shortcuts)
        }

        val actions =
            createListItems(keyMap, showDeviceDescriptors, errorSnapshot)

        return ConfigActionsState.Loaded(
            actions = actions,
            isReorderingEnabled = keyMap.actionList.size > 1,
            shortcuts = shortcuts,
        )
    }

    private fun createListItems(
        keyMap: KeyMap,
        showDeviceDescriptors: Boolean,
        errorSnapshot: ActionErrorSnapshot,
    ): List<ActionListItemModel> {
        val actionErrors = errorSnapshot.getErrors(keyMap.actionList.map { it.data })

        val midDot = getString(R.string.middot)

        return keyMap.actionList.mapIndexed { index, action ->
            val icon: ComposeIconInfo = uiHelper.getIcon(action.data)
            val error: KMError? = actionErrors[action.data]

            val repeatText = uiHelper.getRepeatDescription(keyMap, action)
            val burstText = uiHelper.getBurstDescription(action)
            val holdDownText = uiHelper.getHoldDownDescription(keyMap, action)

            val summary = listOfNotNull(repeatText, burstText, holdDownText)
                .joinToString(" $midDot ")
                .takeIf { it.isNotBlank() }

            ActionListItemModel(
                id = action.uid,
                icon = icon,
                title = uiHelper.getTitle(action, showDeviceDescriptors),
                isCustomName = !action.customName.isNullOrBlank(),
                isEnabled = action.isEnabled,
                summary = summary,
                error = error?.getFullMessage(this),
                isErrorFixable = error?.isFixable ?: true,
                showRepeat = keyMap.isRepeatingActionsAllowed(),
                repeatText = repeatText,
                burstText = burstText,
                showHoldDown = keyMap.isHoldingDownActionAllowed(action),
                holdDownText = holdDownText,
                showDelayChip = index < keyMap.actionList.lastIndex,
                delayBeforeNextAction = action.delayBeforeNextAction,
            )
        }
    }

    private suspend fun buildOptionsState(
        keyMap: State<KeyMap>,
        actionUid: String?,
    ): ActionOptionsState? {
        if (actionUid == null) {
            return null
        }
        val keyMap = keyMap.dataOrNull() ?: return null
        val action = keyMap.actionList.find { it.uid == actionUid } ?: return null

        val allowedRepeatModes = mutableSetOf<RepeatMode>()

        if (keyMap.isRepeatingActionsAllowed()) {
            if (keyMap.isRepeatUntilReleasedAllowed()) {
                allowedRepeatModes.add(RepeatMode.TRIGGER_RELEASED)
            }

            allowedRepeatModes.add(RepeatMode.TRIGGER_PRESSED_AGAIN)
            allowedRepeatModes.add(RepeatMode.LIMIT_REACHED)
        }

        val showDeviceDescriptors = displayAction.showDeviceDescriptors.first()

        val defaultRepeatRate = config.defaultRepeatRate.first()
        val defaultRepeatDelay = config.defaultRepeatDelay.first()
        val defaultHoldDownDuration = config.defaultHoldDownDuration.first()
        val defaultRepeatLimit = if (action.repeatMode == RepeatMode.LIMIT_REACHED) {
            1
        } else {
            Int.MAX_VALUE
        }

        val showRepeatRateWarning =
            keyMap.isRepeatingActionsAllowed() &&
                action.data is ActionData.InputKeyEvent &&
                (action.repeatRate ?: defaultRepeatRate) < 20 &&
                keyMap.trigger.keys.any { it is EvdevTriggerKey }

        return ActionOptionsState(
            title = uiHelper.getTitle(action, showDeviceDescriptors),
            actionTypeTitle = getString(ActionUtils.getTitle(action.data.id)),
            actionTypeIcon = ActionUtils.getComposeIcon(action.data.id),

            showEditButton = action.data.isEditable(),

            showRepeat = keyMap.isRepeatingActionsAllowed(),
            showRepeatRateWarning = showRepeatRateWarning,
            isRepeatChecked = action.repeat,

            showRepeatRate = keyMap.isChangingActionRepeatRateAllowed(action),
            repeatRate = action.repeatRate ?: defaultRepeatRate,
            defaultRepeatRate = defaultRepeatRate,

            showRepeatDelay = keyMap.isChangingActionRepeatDelayAllowed(action),
            repeatDelay = action.repeatDelay ?: defaultRepeatDelay,
            defaultRepeatDelay = defaultRepeatDelay,

            showRepeatLimit = keyMap.isChangingRepeatLimitAllowed(action),
            repeatLimit = action.repeatLimit ?: defaultRepeatLimit,
            defaultRepeatLimit = defaultRepeatLimit,

            allowedRepeatModes = allowedRepeatModes,
            repeatMode = action.repeatMode,

            showHoldDown = keyMap.isHoldingDownActionAllowed(action),
            isHoldDownChecked = action.holdDown,

            showHoldDownDuration = keyMap.isHoldingDownActionBeforeRepeatingAllowed(action),
            holdDownDuration = action.holdDownDuration ?: defaultHoldDownDuration,
            defaultHoldDownDuration = defaultHoldDownDuration,

            holdDownMode = if (action.stopHoldDownWhenTriggerPressedAgain) {
                HoldDownMode.TRIGGER_PRESSED_AGAIN
            } else {
                HoldDownMode.TRIGGER_RELEASED
            },

            multiplier = action.multiplier ?: 1,
            defaultMultiplier = 1,
        )
    }
}

sealed class ConfigActionsState {
    abstract val shortcuts: Set<ShortcutModel<ActionData>>

    data class Empty(override val shortcuts: Set<ShortcutModel<ActionData>> = emptySet()) :
        ConfigActionsState()

    data class Loaded(
        val actions: List<ActionListItemModel> = emptyList(),
        val isReorderingEnabled: Boolean = false,
        override val shortcuts: Set<ShortcutModel<ActionData>> = emptySet(),
    ) : ConfigActionsState()
}
