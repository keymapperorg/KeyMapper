package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.home.ChooseAppStoreModel
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.ShortcutModel
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.isFixable
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.ui.DialogResponse
import io.github.sds100.keymapper.util.ui.LinkType
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.PopupUi
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 22/11/20.
 */

class ConfigActionsViewModel(
    private val coroutineScope: CoroutineScope,
    private val displayAction: DisplayActionUseCase,
    private val createAction: CreateActionUseCase,
    private val testAction: TestActionUseCase,
    private val config: ConfigKeyMapUseCase,
    private val onboarding: OnboardingUseCase,
    resourceProvider: ResourceProvider,
) : CreateActionViewModel by CreateActionViewModelImpl(createAction, resourceProvider),
    ActionOptionsBottomSheetCallback {

    private val uiHelper = ActionUiHelper(displayAction, resourceProvider)

    private val _state = MutableStateFlow<State<ConfigActionsState>>(State.Loading)
    val state = _state.asStateFlow()

    private val shortcuts: StateFlow<Set<ShortcutModel<ActionData>>> =
        config.recentlyUsedActions.map { actions ->
            actions.map(::buildShortcut).toSet()
        }.stateIn(coroutineScope, SharingStarted.Lazily, emptySet())

    val actionOptionsUid = MutableStateFlow<String?>(null)
    val actionOptionsState: StateFlow<ActionOptionsState?> =
        combine(config.keyMap, actionOptionsUid, transform = ::buildOptionsState)
            .stateIn(coroutineScope, SharingStarted.Lazily, null)

    private val actionErrorSnapshot: StateFlow<ActionErrorSnapshot?> =
        displayAction.actionErrorSnapshot.stateIn(coroutineScope, SharingStarted.Lazily, null)

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
        }.launchIn(coroutineScope)
    }

    private suspend fun getActionData(uid: String): ActionData? {
        return config.keyMap.first().dataOrNull()?.actionList?.singleOrNull { it.uid == uid }?.data
    }

    fun onClickShortcut(action: ActionData) {
        coroutineScope.launch {
            config.addAction(action)
        }
    }

    fun onFixError(actionUid: String) {
        coroutineScope.launch {
            val actionData = getActionData(actionUid) ?: return@launch
            val error =
                actionErrorSnapshot.filterNotNull().first().getError(actionData) ?: return@launch

            if (error == Error.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY)) {
                coroutineScope.launch {
                    ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                        resourceProvider = this@ConfigActionsViewModel,
                        popupViewModel = this@ConfigActionsViewModel,
                        neverShowDndTriggerErrorAgain = { displayAction.neverShowDndTriggerError() },
                        fixError = { displayAction.fixError(error) },
                    )
                }
            } else {
                ViewModelHelper.showFixErrorDialog(
                    resourceProvider = this@ConfigActionsViewModel,
                    popupViewModel = this@ConfigActionsViewModel,
                    error,
                ) {
                    displayAction.fixError(error)
                }
            }
        }
    }

    fun onAddActionClick() {
        coroutineScope.launch {
            val actionData = navigate("add_action", NavDestination.ChooseAction) ?: return@launch

            val showInstallShizukuPrompt = onboarding.showInstallShizukuPrompt(actionData)
            val showInstallGuiKeyboardPrompt =
                onboarding.showInstallGuiKeyboardPrompt(actionData)

            when {
                showInstallShizukuPrompt && showInstallGuiKeyboardPrompt ->
                    promptToInstallShizukuOrGuiKeyboard()

                showInstallGuiKeyboardPrompt -> promptToInstallGuiKeyboard()
            }

            config.addAction(actionData)
        }
    }

    fun onMoveAction(fromIndex: Int, toIndex: Int) {
        config.moveAction(fromIndex, toIndex)
    }

    fun onRemoveClick(actionUid: String) {
        config.removeAction(actionUid)
    }

    fun onEditClick(actionUid: String) {
        actionOptionsUid.value = actionUid
    }

    fun onTestClick(actionUid: String) {
        coroutineScope.launch {
            val actionData = getActionData(actionUid) ?: return@launch
            attemptTestAction(actionData)
        }
    }

    override fun onEditClick() {
        val actionUid = actionOptionsUid.value ?: return
        coroutineScope.launch {
            val keyMap = config.keyMap.first().dataOrNull() ?: return@launch

            val oldAction = keyMap.actionList.find { it.uid == actionUid } ?: return@launch
            val newActionData = editAction(oldAction.data)

            if (newActionData != null) {
                actionOptionsUid.update { null }
                config.setActionData(actionUid, newActionData)
            }
        }
    }

    override fun onReplaceClick() {
        val actionUid = actionOptionsUid.value ?: return
        coroutineScope.launch {
            val newActionData =
                navigate("replace_action", NavDestination.ChooseAction) ?: return@launch

            actionOptionsUid.update { null }
            config.setActionData(actionUid, newActionData)
        }
    }

    override fun onRepeatCheckedChange(checked: Boolean) {
        actionOptionsUid.value?.let { uid -> config.setActionRepeatEnabled(uid, checked) }
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

    override fun onHoldDownCheckedChange(checked: Boolean) {
        actionOptionsUid.value?.let { uid -> config.setActionHoldDownEnabled(uid, checked) }
    }

    override fun onHoldDownDurationChanged(duration: Int) {
        actionOptionsUid.value?.let { uid -> config.setActionHoldDownDuration(uid, duration) }
    }

    override fun onDelayBeforeNextActionChanged(delay: Int) {
        actionOptionsUid.value?.let { uid -> config.setDelayBeforeNextAction(uid, delay) }
    }

    override fun onMultiplierChanged(multiplier: Int) {
        actionOptionsUid.value?.let { uid -> config.setActionMultiplier(uid, multiplier) }
    }

    override fun onSelectRepeatMode(repeatMode: RepeatMode) {
        actionOptionsUid.value?.let { uid ->
            when (repeatMode) {
                RepeatMode.TRIGGER_RELEASED -> config.setActionStopRepeatingWhenTriggerReleased(uid)
                RepeatMode.LIMIT_REACHED -> config.setActionStopRepeatingWhenLimitReached(uid)
                RepeatMode.TRIGGER_PRESSED_AGAIN -> config.setActionStopRepeatingWhenTriggerPressedAgain(
                    uid,
                )
            }
        }
    }

    private suspend fun attemptTestAction(actionData: ActionData) {
        testAction.invoke(actionData).onFailure { error ->

            if (error is Error.AccessibilityServiceDisabled) {
                ViewModelHelper.handleAccessibilityServiceStoppedDialog(
                    resourceProvider = this,
                    popupViewModel = this,
                    startService = displayAction::startAccessibilityService,
                )
            }

            if (error is Error.AccessibilityServiceCrashed) {
                ViewModelHelper.handleAccessibilityServiceCrashedSnackBar(
                    resourceProvider = this,
                    popupViewModel = this,
                    restartService = displayAction::restartAccessibilityService,
                    message = R.string.dialog_message_restart_accessibility_service_to_test_action,
                )
            }
        }
    }

    private suspend fun promptToInstallGuiKeyboard() {
        if (onboarding.isTvDevice()) {
            val appStoreModel = ChooseAppStoreModel(
                githubLink = getString(R.string.url_github_keymapper_leanback_keyboard),
            )

            val dialog = PopupUi.ChooseAppStore(
                title = getString(R.string.dialog_title_install_leanback_keyboard),
                message = getString(R.string.dialog_message_install_leanback_keyboard),
                appStoreModel,
                positiveButtonText = getString(R.string.pos_never_show_again),
                negativeButtonText = getString(R.string.neg_cancel),
            )

            val response = showPopup("download_leanback_ime", dialog) ?: return

            if (response == DialogResponse.POSITIVE) {
                onboarding.neverShowGuiKeyboardPromptsAgain()
            }
        } else {
            val appStoreModel = ChooseAppStoreModel(
                playStoreLink = getString(R.string.url_play_store_keymapper_gui_keyboard),
                fdroidLink = getString(R.string.url_fdroid_keymapper_gui_keyboard),
                githubLink = getString(R.string.url_github_keymapper_gui_keyboard),
            )

            val dialog = PopupUi.ChooseAppStore(
                title = getString(R.string.dialog_title_install_gui_keyboard),
                message = getString(R.string.dialog_message_install_gui_keyboard),
                appStoreModel,
                positiveButtonText = getString(R.string.pos_never_show_again),
                negativeButtonText = getString(R.string.neg_cancel),
            )

            val response = showPopup("download_gui_keyboard", dialog) ?: return

            if (response == DialogResponse.POSITIVE) {
                onboarding.neverShowGuiKeyboardPromptsAgain()
            }
        }
    }

    private suspend fun promptToInstallShizukuOrGuiKeyboard() {
        if (onboarding.isTvDevice()) {
            val chooseSolutionDialog = PopupUi.Dialog(
                title = getText(R.string.dialog_title_install_shizuku_or_leanback_keyboard),
                message = getText(R.string.dialog_message_install_shizuku_or_leanback_keyboard),
                positiveButtonText = getString(R.string.dialog_button_install_shizuku),
                negativeButtonText = getString(R.string.dialog_button_install_leanback_keyboard),
                neutralButtonText = getString(R.string.dialog_button_install_nothing),
            )

            val chooseSolutionResponse =
                showPopup("choose_solution", chooseSolutionDialog) ?: return

            when (chooseSolutionResponse) {
                // install shizuku
                DialogResponse.POSITIVE -> {
                    navigate("shizuku", NavDestination.ShizukuSettings)
                    onboarding.neverShowGuiKeyboardPromptsAgain()

                    return
                }
                // do nothing
                DialogResponse.NEUTRAL -> {
                    onboarding.neverShowGuiKeyboardPromptsAgain()
                    return
                }

                // download leanback keyboard
                DialogResponse.NEGATIVE -> {
                    val chooseAppStoreDialog = PopupUi.ChooseAppStore(
                        title = getString(R.string.dialog_title_choose_download_leanback_keyboard),
                        message = getString(R.string.dialog_message_choose_download_leanback_keyboard),
                        model = ChooseAppStoreModel(
                            githubLink = getString(R.string.url_github_keymapper_leanback_keyboard),
                        ),
                        positiveButtonText = getString(R.string.pos_never_show_again),
                        negativeButtonText = getString(R.string.neg_cancel),
                    )

                    val response = showPopup("install_leanback_keyboard", chooseAppStoreDialog)

                    if (response == DialogResponse.POSITIVE) {
                        onboarding.neverShowGuiKeyboardPromptsAgain()
                    }
                }
            }
        } else {
            val chooseSolutionDialog = PopupUi.Dialog(
                title = getText(R.string.dialog_title_install_shizuku_or_gui_keyboard),
                message = getText(R.string.dialog_message_install_shizuku_or_gui_keyboard),
                positiveButtonText = getString(R.string.dialog_button_install_shizuku),
                negativeButtonText = getString(R.string.dialog_button_install_gui_keyboard),
                neutralButtonText = getString(R.string.dialog_button_install_nothing),
            )

            val chooseSolutionResponse =
                showPopup("choose_solution", chooseSolutionDialog) ?: return

            when (chooseSolutionResponse) {
                // install shizuku
                DialogResponse.POSITIVE -> {
                    navigate("shizuku_error", NavDestination.ShizukuSettings)
                    onboarding.neverShowGuiKeyboardPromptsAgain()

                    return
                }
                // do nothing
                DialogResponse.NEUTRAL -> {
                    onboarding.neverShowGuiKeyboardPromptsAgain()
                    return
                }

                // download gui keyboard
                DialogResponse.NEGATIVE -> {
                    val chooseAppStoreDialog = PopupUi.ChooseAppStore(
                        title = getString(R.string.dialog_title_choose_download_gui_keyboard),
                        message = getString(R.string.dialog_message_choose_download_gui_keyboard),
                        model = ChooseAppStoreModel(
                            playStoreLink = getString(R.string.url_play_store_keymapper_gui_keyboard),
                            fdroidLink = getString(R.string.url_fdroid_keymapper_gui_keyboard),
                            githubLink = getString(R.string.url_github_keymapper_gui_keyboard),
                        ),
                        positiveButtonText = getString(R.string.pos_never_show_again),
                        negativeButtonText = getString(R.string.neg_cancel),
                    )

                    val response = showPopup("install_gui_keyboard", chooseAppStoreDialog)

                    if (response == DialogResponse.POSITIVE) {
                        onboarding.neverShowGuiKeyboardPromptsAgain()
                    }
                }
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

        val actions = createListItems(keyMap, showDeviceDescriptors, errorSnapshot, shortcuts.size)

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
        shortcutCount: Int,
    ): List<ActionListItemModel> {
        return keyMap.actionList.mapIndexed { index, action ->

            val title: String = if (action.multiplier != null && action.multiplier > 1) {
                val multiplier = action.multiplier
                "${multiplier}x ${uiHelper.getTitle(action.data, showDeviceDescriptors)}"
            } else {
                uiHelper.getTitle(action.data, showDeviceDescriptors)
            }

            val icon: ComposeIconInfo = uiHelper.getIcon(action.data)
            val error: Error? = errorSnapshot.getError(action.data)

            val extraInfo = buildString {
                val midDot = getString(R.string.middot)

                uiHelper.getOptionLabels(keyMap, action).forEachIndexed { index, label ->
                    if (index != 0) {
                        append(" $midDot ")
                    }

                    append(label)
                }

                action.delayBeforeNextAction.apply {
                    if (keyMap.isDelayBeforeNextActionAllowed() && action.delayBeforeNextAction != null) {
                        if (this@buildString.isNotBlank()) {
                            append(" $midDot ")
                        }

                        append(
                            getString(
                                R.string.action_title_wait,
                                action.delayBeforeNextAction,
                            ),
                        )
                    }
                }
            }.takeIf { it.isNotBlank() }

            val linkType = when {
                index == keyMap.actionList.lastIndex && shortcutCount > 0 -> LinkType.PLUS
                index < keyMap.actionList.lastIndex -> LinkType.ARROW
                else -> LinkType.HIDDEN
            }

            ActionListItemModel(
                id = action.uid,
                icon = icon,
                text = title,
                secondaryText = extraInfo,
                error = error?.getFullMessage(this),
                isErrorFixable = error?.isFixable ?: true,
                linkType = linkType,
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

        if (keyMap.isChangingRepeatModeAllowed(action)) {
            allowedRepeatModes.add(RepeatMode.TRIGGER_RELEASED)
            allowedRepeatModes.add(RepeatMode.TRIGGER_PRESSED_AGAIN)
            allowedRepeatModes.add(RepeatMode.LIMIT_REACHED)
        }

        val defaultRepeatRate = config.defaultRepeatRate.first()
        val defaultRepeatDelay = config.defaultRepeatDelay.first()
        val defaultHoldDownDuration = config.defaultHoldDownDuration.first()
        val defaultRepeatLimit = if (action.repeatMode == RepeatMode.LIMIT_REACHED) {
            1
        } else {
            Int.MAX_VALUE
        }

        return ActionOptionsState(
            showEditButton = action.data.isEditable(),

            showRepeat = keyMap.isRepeatingActionsAllowed(),
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

            showHoldDownMode = keyMap.isStopHoldingDownActionWhenTriggerPressedAgainAllowed(action),
            holdDownMode = if (action.stopHoldDownWhenTriggerPressedAgain) {
                HoldDownMode.TRIGGER_PRESSED_AGAIN
            } else {
                HoldDownMode.TRIGGER_RELEASED
            },

            showDelayBeforeNextAction = keyMap.isDelayBeforeNextActionAllowed(),
            delayBeforeNextAction = action.delayBeforeNextAction ?: 0,
            defaultDelayBeforeNextAction = 0,

            multiplier = action.multiplier ?: 1,
            defaultMultiplier = 1,
        )
    }
}

sealed class ConfigActionsState {
    data class Empty(
        val shortcuts: Set<ShortcutModel<ActionData>> = emptySet(),
    ) : ConfigActionsState()

    data class Loaded(
        val actions: List<ActionListItemModel> = emptyList(),
        val isReorderingEnabled: Boolean = false,
        val shortcuts: Set<ShortcutModel<ActionData>> = emptySet(),
    ) : ConfigActionsState()
}

data class ActionListItemModel(
    val id: String,
    val icon: ComposeIconInfo,
    val text: String,
    val secondaryText: String?,
    val error: String? = null,
    val isErrorFixable: Boolean = true,
    val linkType: LinkType = LinkType.HIDDEN,
)

data class ActionOptionsState(
    val showEditButton: Boolean,

    val showRepeat: Boolean,
    val isRepeatChecked: Boolean,

    val showRepeatRate: Boolean,
    val repeatRate: Int,
    val defaultRepeatRate: Int,

    val showRepeatDelay: Boolean,
    val repeatDelay: Int,
    val defaultRepeatDelay: Int,

    val showRepeatLimit: Boolean,
    val repeatLimit: Int,
    val defaultRepeatLimit: Int,

    val allowedRepeatModes: Set<RepeatMode>,
    val repeatMode: RepeatMode,

    val showHoldDown: Boolean,
    val isHoldDownChecked: Boolean,

    val showHoldDownDuration: Boolean,
    val holdDownDuration: Int,
    val defaultHoldDownDuration: Int,

    val showHoldDownMode: Boolean,
    val holdDownMode: HoldDownMode,

    val showDelayBeforeNextAction: Boolean,
    val delayBeforeNextAction: Int,
    val defaultDelayBeforeNextAction: Int,

    val multiplier: Int,
    val defaultMultiplier: Int,
)
