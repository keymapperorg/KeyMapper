package io.github.sds100.keymapper.base.actions.keyevent

import android.os.Build
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.onboarding.SetupAccessibilityServiceDelegate
import io.github.sds100.keymapper.base.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.base.trigger.SetupInputMethodUseCase
import io.github.sds100.keymapper.base.utils.ExpertModeStatus
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@ViewModelScoped
class FixKeyEventActionDelegateImpl @Inject constructor(
    @Named("viewmodel")
    val viewModelScope: CoroutineScope,
    val controlAccessibilityServiceUseCase: ControlAccessibilityServiceUseCase,
    val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    val setupInputMethodUseCase: SetupInputMethodUseCase,
    val preferenceRepository: PreferenceRepository,
    val setupAccessibilityServiceDelegate: SetupAccessibilityServiceDelegate,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
) : FixKeyEventActionDelegate,
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    private val expertModeStatus: Flow<ExpertModeStatus> =
        systemBridgeConnectionManager.connectionState.map { state ->
            when (state) {
                is SystemBridgeConnectionState.Connected -> ExpertModeStatus.ENABLED
                is SystemBridgeConnectionState.Disconnected -> ExpertModeStatus.DISABLED
            }
        }

    private val isExpertModeSelected: Flow<Boolean> =
        preferenceRepository.get(Keys.keyEventActionsUseSystemBridge)
            .map { it ?: PreferenceDefaults.KEY_EVENT_ACTIONS_USE_SYSTEM_BRIDGE }

    private val showBottomSheet: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val fixKeyEventActionState: StateFlow<FixKeyEventActionState?> =
        showBottomSheet.flatMapLatest { showBottomSheet ->
            if (showBottomSheet) {
                buildStateFlow()
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildStateFlow(): Flow<FixKeyEventActionState> {
        return isExpertModeSelected.flatMapLatest { isExpertModeSelected ->
            if (isExpertModeSelected) {
                combine(
                    expertModeStatus,
                    controlAccessibilityServiceUseCase.serviceState,
                ) { expertModeStatus, serviceState ->
                    FixKeyEventActionState.ExpertMode(
                        expertModeStatus = expertModeStatus,
                        isAccessibilityServiceEnabled =
                        serviceState == AccessibilityServiceState.ENABLED,
                    )
                }
            } else {
                combine(
                    expertModeStatus,
                    setupInputMethodUseCase.isEnabled,
                    setupInputMethodUseCase.isChosen,
                    controlAccessibilityServiceUseCase.serviceState,
                    preferenceRepository.get(Keys.changeImeOnInputFocus),
                ) { expertModeStatus, isEnabled, isChosen, serviceState, changeImeOnInputFocus ->
                    val enablingRequiresUserInput =
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

                    FixKeyEventActionState.InputMethod(
                        isEnabled = isEnabled,
                        isChosen = isChosen,
                        enablingRequiresUserInput = enablingRequiresUserInput,
                        isAccessibilityServiceEnabled =
                        serviceState == AccessibilityServiceState.ENABLED,
                        expertModeStatus = expertModeStatus,
                        isAutoSwitchImeEnabled = changeImeOnInputFocus
                            ?: PreferenceDefaults.CHANGE_IME_ON_INPUT_FOCUS,
                    )
                }
            }
        }
    }

    override fun showFixKeyEventActionBottomSheet() {
        showBottomSheet.value = true
    }

    override fun dismissFixKeyEventActionBottomSheet() {
        showBottomSheet.value = false
    }

    override fun onEnableAccessibilityServiceClick() {
        viewModelScope.launch {
            setupAccessibilityServiceDelegate.showEnableAccessibilityServiceDialog()
        }
    }

    override fun onEnableExpertModeForKeyEventActionsClick() {
        viewModelScope.launch {
            navigate("fix_key_event_action_expert_mode", NavDestination.ExpertMode)
        }
    }

    override fun onEnableImeClick() {
        viewModelScope.launch {
            setupInputMethodUseCase.enableInputMethod()
        }
    }

    override fun onChooseImeClick() {
        viewModelScope.launch {
            setupInputMethodUseCase.chooseInputMethod().onFailure {
                Timber.e("Failed to choose input method when fixing key event action. Error: $it")
            }
        }
    }

    override fun onSelectExpertMode() {
        preferenceRepository.set(Keys.keyEventActionsUseSystemBridge, true)
    }

    override fun onSelectInputMethod() {
        preferenceRepository.set(Keys.keyEventActionsUseSystemBridge, false)
    }

    override fun onAutoSwitchImeCheckedChange(checked: Boolean) {
        viewModelScope.launch {
            preferenceRepository.set(Keys.changeImeOnInputFocus, checked)
        }
    }
}

interface FixKeyEventActionDelegate {
    val fixKeyEventActionState: StateFlow<FixKeyEventActionState?>

    fun showFixKeyEventActionBottomSheet()
    fun dismissFixKeyEventActionBottomSheet()
    fun onEnableAccessibilityServiceClick()
    fun onEnableExpertModeForKeyEventActionsClick()
    fun onEnableImeClick()
    fun onChooseImeClick()
    fun onSelectExpertMode()
    fun onSelectInputMethod()
    fun onAutoSwitchImeCheckedChange(checked: Boolean)
}
