package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.home.ChooseAppStoreModel
import io.github.sds100.keymapper.mappings.ConfigMappingUseCase
import io.github.sds100.keymapper.mappings.DisplayActionUseCase
import io.github.sds100.keymapper.mappings.Mapping
import io.github.sds100.keymapper.mappings.isDelayBeforeNextActionAllowed
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.isFixable
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.ui.DialogResponse
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 22/11/20.
 */

class ConfigActionsViewModel<A : Action, M : Mapping<A>>(
    private val coroutineScope: CoroutineScope,
    private val displayActionUseCase: DisplayActionUseCase,
    private val testActionUseCase: TestActionUseCase,
    private val config: ConfigMappingUseCase<A, M>,
    private val uiHelper: ActionUiHelper<M, A>,
    private val onboardingUseCase: OnboardingUseCase,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    private val _state = MutableStateFlow<State<List<ActionListItem>>>(State.Loading)
    val state = _state.asStateFlow()

    private val _openEditOptions = MutableSharedFlow<String>()

    /**
     * value is the uid of the action
     */
    val openEditOptions = _openEditOptions.asSharedFlow()

    private val _navigateToShizukuSetup = MutableSharedFlow<Unit>()
    val navigateToShizukuSetup = _navigateToShizukuSetup.asSharedFlow()

    init {
        val rebuildUiState = MutableSharedFlow<State<M>>()

        combine(
            rebuildUiState,
            displayActionUseCase.showDeviceDescriptors,
        ) { mappingState, showDeviceDescriptors ->
            _state.value = mappingState.mapData { mapping ->
                createListItems(mapping, showDeviceDescriptors)
            }
        }.flowOn(Dispatchers.Default).launchIn(coroutineScope)

        coroutineScope.launch {
            config.mapping.collectLatest {
                rebuildUiState.emit(it)
            }
        }

        coroutineScope.launch {
            displayActionUseCase.invalidateActionErrors.collectLatest {
                rebuildUiState.emit(config.mapping.firstOrNull() ?: return@collectLatest)
            }
        }
    }

    fun onModelClick(uid: String) {
        coroutineScope.launch(Dispatchers.Default) {
            config.mapping.first().ifIsData { data ->
                val actionData = data.actionList.singleOrNull { it.uid == uid }?.data
                    ?: return@launch

                val error = displayActionUseCase.getError(actionData)

                when {
                    error == null -> attemptTestAction(actionData)
                    error.isFixable -> displayActionUseCase.fixError(error)
                }
            }
        }
    }

    fun onAddActionClick() {
        coroutineScope.launch {
            val actionData = navigate("add_action", NavDestination.ChooseAction) ?: return@launch

            val showInstallShizukuPrompt = onboardingUseCase.showInstallShizukuPrompt(actionData)
            val showInstallGuiKeyboardPrompt =
                onboardingUseCase.showInstallGuiKeyboardPrompt(actionData)

            when {
                showInstallShizukuPrompt && showInstallGuiKeyboardPrompt ->
                    promptToInstallShizukuOrGuiKeyboard()

                showInstallGuiKeyboardPrompt -> promptToInstallGuiKeyboard()
            }

            config.addAction(actionData)
        }
    }

    fun moveAction(fromIndex: Int, toIndex: Int) {
        config.moveAction(fromIndex, toIndex)
    }

    fun onRemoveClick(actionUid: String) {
        config.removeAction(actionUid)
    }

    fun editAction(actionUid: String) {
        runBlocking { _openEditOptions.emit(actionUid) }
    }

    private suspend fun attemptTestAction(actionData: ActionData) {
        testActionUseCase.invoke(actionData).onFailure { error ->

            if (error is Error.AccessibilityServiceDisabled) {
                ViewModelHelper.handleAccessibilityServiceStoppedSnackBar(
                    resourceProvider = this,
                    popupViewModel = this,
                    startService = displayActionUseCase::startAccessibilityService,
                    message = R.string.dialog_message_enable_accessibility_service_to_test_action,
                )
            }

            if (error is Error.AccessibilityServiceCrashed) {
                ViewModelHelper.handleAccessibilityServiceCrashedSnackBar(
                    resourceProvider = this,
                    popupViewModel = this,
                    restartService = displayActionUseCase::restartAccessibilityService,
                    message = R.string.dialog_message_restart_accessibility_service_to_test_action,
                )
            }
        }
    }

    private suspend fun promptToInstallGuiKeyboard() {
        if (onboardingUseCase.isTvDevice()) {
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
                onboardingUseCase.neverShowGuiKeyboardPromptsAgain()
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
                onboardingUseCase.neverShowGuiKeyboardPromptsAgain()
            }
        }
    }

    private suspend fun promptToInstallShizukuOrGuiKeyboard() {
        if (onboardingUseCase.isTvDevice()) {
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
                    _navigateToShizukuSetup.emit(Unit)
                    onboardingUseCase.neverShowGuiKeyboardPromptsAgain()

                    return
                }
                // do nothing
                DialogResponse.NEUTRAL -> {
                    onboardingUseCase.neverShowGuiKeyboardPromptsAgain()
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
                        onboardingUseCase.neverShowGuiKeyboardPromptsAgain()
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
                    _navigateToShizukuSetup.emit(Unit)
                    onboardingUseCase.neverShowGuiKeyboardPromptsAgain()

                    return
                }
                // do nothing
                DialogResponse.NEUTRAL -> {
                    onboardingUseCase.neverShowGuiKeyboardPromptsAgain()
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
                        onboardingUseCase.neverShowGuiKeyboardPromptsAgain()
                    }
                }
            }
        }
    }

    private fun createListItems(mapping: M, showDeviceDescriptors: Boolean): List<ActionListItem> {
        val actionCount = mapping.actionList.size

        return mapping.actionList.map { action ->

            val title: String = if (action.multiplier != null && action.multiplier!! > 1) {
                val multiplier = action.multiplier
                "${multiplier}x ${uiHelper.getTitle(action.data, showDeviceDescriptors)}"
            } else {
                uiHelper.getTitle(action.data, showDeviceDescriptors)
            }

            val icon: IconInfo? = uiHelper.getIcon(action.data)
            val error: Error? = uiHelper.getError(action.data)

            val extraInfo = buildString {
                val midDot = getString(R.string.middot)

                uiHelper.getOptionLabels(mapping, action).forEachIndexed { index, label ->
                    if (index != 0) {
                        append(" $midDot ")
                    }

                    append(label)
                }

                action.delayBeforeNextAction.apply {
                    if (mapping.isDelayBeforeNextActionAllowed() && action.delayBeforeNextAction != null) {
                        if (this@buildString.isNotBlank()) {
                            append(" $midDot ")
                        }

                        append(
                            getString(
                                R.string.action_title_wait,
                                action.delayBeforeNextAction!!,
                            ),
                        )
                    }
                }
            }.takeIf { it.isNotBlank() }

            ActionListItem(
                id = action.uid,
                tintType = if (error != null) {
                    TintType.Error
                } else {
                    icon?.tintType ?: TintType.None
                },
                icon = if (error != null) {
                    getDrawable(R.drawable.ic_baseline_error_outline_24)
                } else {
                    icon?.drawable
                },
                title = title,
                extraInfo = extraInfo,
                errorMessage = error?.getFullMessage(this),
                dragAndDrop = actionCount > 1,
            )
        }
    }
}
