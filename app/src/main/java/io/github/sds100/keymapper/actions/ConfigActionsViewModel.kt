package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.ConfigMappingUseCase
import io.github.sds100.keymapper.mappings.DisplayActionUseCase
import io.github.sds100.keymapper.mappings.Mapping
import io.github.sds100.keymapper.mappings.isDelayBeforeNextActionAllowed
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Created by sds100 on 22/11/20.
 */

class ConfigActionsViewModel<A : Action, M : Mapping<A>>(
    private val coroutineScope: CoroutineScope,
    private val displayActionUseCase: DisplayActionUseCase,
    private val testAction: TestActionUseCase,
    private val config: ConfigMappingUseCase<A, M>,
    private val uiHelper: ActionUiHelper<M, A>,
    private val onboardingUseCase: OnboardingUseCase,
    resourceProvider: ResourceProvider
) : ResourceProvider by resourceProvider, PopupViewModel by PopupViewModelImpl() {

    private val _state = MutableStateFlow<State<List<ActionListItem>>>(State.Loading)
    val state = _state.asStateFlow()

    private val _openEditOptions = MutableSharedFlow<String>()

    /**
     * value is the uid of the action
     */
    val openEditOptions = _openEditOptions.asSharedFlow()

    init {
        val rebuildUiState = MutableSharedFlow<State<M>>()

        combine(
            rebuildUiState,
            displayActionUseCase.showDeviceDescriptors
        ) { mappingState, showDeviceDescriptors ->
            _state.value = mappingState.mapData { mapping ->
                createListItems(mapping, showDeviceDescriptors)
            }
        }.launchIn(coroutineScope)

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
                    error == null -> testAction(actionData).onFailure { error ->

                        if (error is Error.AccessibilityServiceDisabled) {

                            val snackBar = PopupUi.SnackBar(
                                message = getString(R.string.dialog_message_enable_accessibility_service_to_test_action),
                                actionText = getString(R.string.pos_turn_on)
                            )

                            val response = showPopup("enable_service", snackBar)

                            if (response != null) {
                                displayActionUseCase.fixError(Error.AccessibilityServiceDisabled)
                            }
                        }

                        if (error is Error.AccessibilityServiceCrashed) {

                            val snackBar = PopupUi.SnackBar(
                                message = getString(R.string.dialog_message_restart_accessibility_service_to_test_action),
                                actionText = getString(R.string.pos_restart)
                            )

                            val response = showPopup("restart_service", snackBar)

                            if (response != null) {
                                displayActionUseCase.fixError(Error.AccessibilityServiceCrashed)
                            }
                        }
                    }

                    error.isFixable -> displayActionUseCase.fixError(error)
                }
            }
        }
    }

    fun addAction(data: ActionData) {
        coroutineScope.launch {
            if (!onboardingUseCase.showGuiKeyboardPrompt.first()) {
                return@launch
            }

            if (data is KeyEventAction || data is TextAction) {
                val response = showPopup("install_gui_keyboard", PopupUi.InstallCompatibleOnScreenKeyboard)

                if (response == DialogResponse.POSITIVE) {
                    onboardingUseCase.neverShowGuiKeyboardPromptsAgain()
                }
            }
        }

        config.addAction(data)
    }

    fun moveAction(fromIndex: Int, toIndex: Int) {
        config.moveAction(fromIndex, toIndex)
    }

    fun onRemoveClick(actionUid: String) {
        config.removeAction(actionUid)
    }

    fun editOptions(actionUid: String) {
        runBlocking { _openEditOptions.emit(actionUid) }
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
                                action.delayBeforeNextAction!!
                            )
                        )
                    }
                }
            }.takeIf { it.isNotBlank() }

            ActionListItem(
                id = action.uid,
                tintType = if (error != null) {
                    TintType.ERROR
                } else {
                    icon?.tintType ?: TintType.NONE
                },
                icon = if (error != null) {
                    getDrawable(R.drawable.ic_baseline_error_outline_24)
                } else {
                    icon?.drawable
                },
                title = title,
                extraInfo = extraInfo,
                errorMessage = error?.getFullMessage(this),
                dragAndDrop = actionCount > 1
            )
        }
    }
}