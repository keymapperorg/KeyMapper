package io.github.sds100.keymapper.base.actions.keyevent

import android.os.Build
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.base.trigger.ProModeStatus
import io.github.sds100.keymapper.base.trigger.SetupInputMethodUseCase
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ViewModelHelper
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@ViewModelScoped
class FixKeyEventActionDelegateImpl @Inject constructor(
    @Named("viewmodel")
    val viewModelScope: CoroutineScope,
    val controlAccessibilityServiceUseCase: ControlAccessibilityServiceUseCase,
    val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    val setupInputMethodUseCase: SetupInputMethodUseCase,
    val preferenceRepository: PreferenceRepository,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
) : FixKeyEventActionDelegate,
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    private val proModeStatus: Flow<ProModeStatus> =
        if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
            systemBridgeConnectionManager.connectionState.map { state ->
                when (state) {
                    is SystemBridgeConnectionState.Connected -> ProModeStatus.ENABLED
                    is SystemBridgeConnectionState.Disconnected -> ProModeStatus.DISABLED
                }
            }
        } else {
            flowOf(ProModeStatus.UNSUPPORTED)
        }

    private val isProModeSelected: Flow<Boolean> =
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
        return isProModeSelected.flatMapLatest { isProModeSelected ->
            if (isProModeSelected) {
                combine(
                    proModeStatus,
                    controlAccessibilityServiceUseCase.serviceState
                ) { proModeStatus, serviceState ->
                    FixKeyEventActionState.ProMode(
                        proModeStatus = proModeStatus,
                        isAccessibilityServiceEnabled = serviceState == AccessibilityServiceState.ENABLED
                    )
                }
            } else {
                combine(
                    setupInputMethodUseCase.isEnabled,
                    setupInputMethodUseCase.isChosen,
                    controlAccessibilityServiceUseCase.serviceState
                ) { isEnabled, isChosen, serviceState ->
                    val enablingRequiresUserInput =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

                    FixKeyEventActionState.InputMethod(
                        isEnabled = isEnabled,
                        isChosen = isChosen,
                        enablingRequiresUserInput = enablingRequiresUserInput,
                        isAccessibilityServiceEnabled = serviceState == AccessibilityServiceState.ENABLED
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
            val state = controlAccessibilityServiceUseCase.serviceState.first()

            if (state == AccessibilityServiceState.DISABLED) {
                ViewModelHelper.handleAccessibilityServiceStoppedDialog(
                    resourceProvider = this@FixKeyEventActionDelegateImpl,
                    dialogProvider = this@FixKeyEventActionDelegateImpl,
                    startService = controlAccessibilityServiceUseCase::startService,
                )
            } else if (state == AccessibilityServiceState.CRASHED) {
                ViewModelHelper.handleAccessibilityServiceCrashedDialog(
                    resourceProvider = this@FixKeyEventActionDelegateImpl,
                    dialogProvider = this@FixKeyEventActionDelegateImpl,
                    restartService = controlAccessibilityServiceUseCase::restartService,
                )
            }
        }
    }

    override fun onEnableProModeForKeyEventActionsClick() {
        viewModelScope.launch {
            navigate("fix_key_event_action_pro_mode", NavDestination.ProMode)
        }
    }

    override fun onEnableImeClick() {
        viewModelScope.launch {
            setupInputMethodUseCase.enableInputMethod()
        }
    }

    override fun onChooseImeClick() {
        viewModelScope.launch {
            setupInputMethodUseCase.chooseInputMethod()
        }
    }

    override fun onSelectProMode() {
        preferenceRepository.set(Keys.keyEventActionsUseSystemBridge, true)
    }

    override fun onSelectInputMethod() {
        preferenceRepository.set(Keys.keyEventActionsUseSystemBridge, false)
    }
}

interface FixKeyEventActionDelegate {
    val fixKeyEventActionState: StateFlow<FixKeyEventActionState?>

    fun showFixKeyEventActionBottomSheet()
    fun dismissFixKeyEventActionBottomSheet()
    fun onEnableAccessibilityServiceClick()
    fun onEnableProModeForKeyEventActionsClick()
    fun onEnableImeClick()
    fun onChooseImeClick()

    fun onSelectProMode()
    fun onSelectInputMethod()
}