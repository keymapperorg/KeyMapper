package io.github.sds100.keymapper.base.trigger

import android.os.Build
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.onboarding.SetupAccessibilityServiceDelegate
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.utils.ProModeStatus
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.AccessibilityServiceError
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalCoroutinesApi::class)
@ViewModelScoped
class TriggerSetupDelegateImpl @Inject constructor(
    @Named("viewmodel")
    val viewModelScope: CoroutineScope,
    val setupAccessibilityServiceDelegate: SetupAccessibilityServiceDelegate,
    val recordTriggerController: RecordTriggerController,
    val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    val configTriggerUseCase: ConfigTriggerUseCase,
    val setupInputMethodUseCase: SetupInputMethodUseCase,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
) : TriggerSetupDelegate,
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider,
    SetupAccessibilityServiceDelegate by setupAccessibilityServiceDelegate {

    private val currentSetupShortcut: MutableStateFlow<TriggerSetupShortcut?> =
        MutableStateFlow(null)

    private val isScreenOffChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val selectedFingerprintGestureType: MutableStateFlow<FingerprintGestureType> =
        MutableStateFlow(FingerprintGestureType.SWIPE_DOWN)

    private val selectedGamePadType: MutableStateFlow<TriggerSetupState.Gamepad.Type> =
        MutableStateFlow(TriggerSetupState.Gamepad.Type.DPAD)

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

    override val triggerSetupState: StateFlow<TriggerSetupState?> =
        currentSetupShortcut.flatMapLatest { shortcut ->
            if (shortcut == null) {
                flowOf(null)
            } else {
                when (shortcut) {
                    TriggerSetupShortcut.VOLUME -> buildSetupVolumeTriggerFlow()
                    TriggerSetupShortcut.POWER -> buildSetupPowerTriggerFlow()
                    TriggerSetupShortcut.FINGERPRINT_GESTURE -> buildSetupFingerprintGestureFlow()
                    TriggerSetupShortcut.KEYBOARD -> buildSetupKeyboardTriggerFlow()
                    TriggerSetupShortcut.MOUSE -> buildSetupMouseTriggerFlow()
                    TriggerSetupShortcut.GAMEPAD -> buildSetupGamepadTriggerFlow()
                    TriggerSetupShortcut.OTHER -> buildSetupOtherTriggerFlow()
                    TriggerSetupShortcut.NOT_DETECTED -> buildSetupNotDetectedFlow()

                    else -> throw UnsupportedOperationException("Unhandled shortcut: $shortcut")
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    override fun showTriggerSetup(shortcut: TriggerSetupShortcut) {
        currentSetupShortcut.value = shortcut
    }

    private fun buildSetupVolumeTriggerFlow(): Flow<TriggerSetupState> {
        return combine(
            accessibilityServiceState,
            isScreenOffChecked,
            recordTriggerController.state,
            proModeStatus,
        ) { serviceState, isScreenOffChecked, recordTriggerState, proModeStatus ->
            val areRequirementsMet = if (isScreenOffChecked) {
                serviceState == AccessibilityServiceState.ENABLED && proModeStatus == ProModeStatus.ENABLED
            } else {
                serviceState == AccessibilityServiceState.ENABLED
            }

            TriggerSetupState.Volume(
                isAccessibilityServiceEnabled = serviceState == AccessibilityServiceState.ENABLED,
                isScreenOffChecked = isScreenOffChecked,
                proModeStatus = proModeStatus,
                areRequirementsMet = areRequirementsMet,
                recordTriggerState = recordTriggerState,
            )
        }
    }

    private fun buildSetupGamepadTriggerFlow(): Flow<TriggerSetupState> {
        return selectedGamePadType.flatMapLatest { selectedGamepadType ->
            when (selectedGamepadType) {
                TriggerSetupState.Gamepad.Type.DPAD -> {
                    combine(
                        accessibilityServiceState,
                        setupInputMethodUseCase.isEnabled,
                        setupInputMethodUseCase.isChosen,
                        recordTriggerController.state,
                    ) { serviceState, isImeEnabled, isImeChosen, recordTriggerState ->
                        val areRequirementsMet =
                            serviceState == AccessibilityServiceState.ENABLED && isImeEnabled && isImeChosen

                        TriggerSetupState.Gamepad.Dpad(
                            isAccessibilityServiceEnabled = serviceState == AccessibilityServiceState.ENABLED,
                            isImeEnabled = isImeEnabled,
                            isImeChosen = isImeChosen,
                            areRequirementsMet = areRequirementsMet,
                            recordTriggerState = recordTriggerState,
                            enablingRequiresUserInput = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
                        )
                    }
                }

                TriggerSetupState.Gamepad.Type.SIMPLE_BUTTONS -> {
                    combine(
                        accessibilityServiceState,
                        isScreenOffChecked,
                        recordTriggerController.state,
                        proModeStatus,
                    ) { serviceState, isScreenOffChecked, recordTriggerState, proModeStatus ->
                        val areRequirementsMet = if (isScreenOffChecked) {
                            serviceState == AccessibilityServiceState.ENABLED && proModeStatus == ProModeStatus.ENABLED
                        } else {
                            serviceState == AccessibilityServiceState.ENABLED
                        }

                        TriggerSetupState.Gamepad.SimpleButtons(
                            isAccessibilityServiceEnabled = serviceState == AccessibilityServiceState.ENABLED,
                            isScreenOffChecked = isScreenOffChecked,
                            proModeStatus = proModeStatus,
                            areRequirementsMet = areRequirementsMet,
                            recordTriggerState = recordTriggerState,
                        )
                    }
                }
            }
        }
    }

    private fun buildSetupOtherTriggerFlow(): Flow<TriggerSetupState> {
        return combine(
            accessibilityServiceState,
            isScreenOffChecked,
            recordTriggerController.state,
            proModeStatus,
        ) { serviceState, isScreenOffChecked, recordTriggerState, proModeStatus ->
            val areRequirementsMet = if (isScreenOffChecked) {
                serviceState == AccessibilityServiceState.ENABLED && proModeStatus == ProModeStatus.ENABLED
            } else {
                serviceState == AccessibilityServiceState.ENABLED
            }

            TriggerSetupState.Other(
                isAccessibilityServiceEnabled = serviceState == AccessibilityServiceState.ENABLED,
                isScreenOffChecked = isScreenOffChecked,
                proModeStatus = proModeStatus,
                areRequirementsMet = areRequirementsMet,
                recordTriggerState = recordTriggerState,
            )
        }
    }

    private fun buildSetupNotDetectedFlow(): Flow<TriggerSetupState> {
        return combine(
            accessibilityServiceState,
            recordTriggerController.state,
            proModeStatus,
        ) { serviceState, recordTriggerState, proModeStatus ->
            val areRequirementsMet =
                serviceState == AccessibilityServiceState.ENABLED && proModeStatus == ProModeStatus.ENABLED

            TriggerSetupState.NotDetected(
                isAccessibilityServiceEnabled = serviceState == AccessibilityServiceState.ENABLED,
                proModeStatus = proModeStatus,
                areRequirementsMet = areRequirementsMet,
                recordTriggerState = recordTriggerState,
            )
        }
    }

    private fun buildSetupKeyboardTriggerFlow(): Flow<TriggerSetupState> {
        return combine(
            accessibilityServiceState,
            isScreenOffChecked,
            recordTriggerController.state,
            proModeStatus,
        ) { serviceState, isScreenOffChecked, recordTriggerState, proModeStatus ->
            val areRequirementsMet = if (isScreenOffChecked) {
                serviceState == AccessibilityServiceState.ENABLED && proModeStatus == ProModeStatus.ENABLED
            } else {
                serviceState == AccessibilityServiceState.ENABLED
            }

            TriggerSetupState.Keyboard(
                isAccessibilityServiceEnabled = serviceState == AccessibilityServiceState.ENABLED,
                isScreenOffChecked = isScreenOffChecked,
                proModeStatus = proModeStatus,
                areRequirementsMet = areRequirementsMet,
                recordTriggerState = recordTriggerState,
            )
        }
    }

    private fun buildSetupFingerprintGestureFlow(): Flow<TriggerSetupState> {
        return combine(
            accessibilityServiceState,
            selectedFingerprintGestureType,
        ) { serviceState, gestureType ->
            val areRequirementsMet = serviceState == AccessibilityServiceState.ENABLED

            TriggerSetupState.FingerprintGesture(
                isAccessibilityServiceEnabled = serviceState == AccessibilityServiceState.ENABLED,
                areRequirementsMet = areRequirementsMet,
                selectedType = gestureType,
            )
        }
    }

    private fun buildSetupPowerTriggerFlow(): Flow<TriggerSetupState> {
        return combine(
            accessibilityServiceState,
            recordTriggerController.state,
            proModeStatus,
        ) { serviceState, recordTriggerState, proModeStatus ->
            val areRequirementsMet =
                serviceState == AccessibilityServiceState.ENABLED && proModeStatus == ProModeStatus.ENABLED

            val remapStatus = if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
                if (areRequirementsMet) {
                    RemapStatus.SUPPORTED
                } else {
                    RemapStatus.UNCERTAIN
                }
            } else {
                RemapStatus.UNSUPPORTED
            }

            TriggerSetupState.Power(
                isAccessibilityServiceEnabled = serviceState == AccessibilityServiceState.ENABLED,
                proModeStatus = proModeStatus,
                areRequirementsMet = areRequirementsMet,
                recordTriggerState = recordTriggerState,
                remapStatus = remapStatus,
            )
        }
    }

    private fun buildSetupMouseTriggerFlow(): Flow<TriggerSetupState> {
        return combine(
            accessibilityServiceState,
            recordTriggerController.state,
            proModeStatus,
        ) { serviceState, recordTriggerState, proModeStatus ->
            val areRequirementsMet =
                serviceState == AccessibilityServiceState.ENABLED && proModeStatus == ProModeStatus.ENABLED

            val remapStatus = if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
                if (areRequirementsMet) {
                    RemapStatus.SUPPORTED
                } else {
                    RemapStatus.UNCERTAIN
                }
            } else {
                RemapStatus.UNSUPPORTED
            }

            TriggerSetupState.Mouse(
                isAccessibilityServiceEnabled = serviceState == AccessibilityServiceState.ENABLED,
                proModeStatus = proModeStatus,
                areRequirementsMet = areRequirementsMet,
                recordTriggerState = recordTriggerState,
                remapStatus = remapStatus,
            )
        }
    }

    override fun onEnableAccessibilityServiceClick() {
        viewModelScope.launch {
            showEnableAccessibilityServiceDialog()
        }
    }

    override fun onEnableProModeClick() {
        viewModelScope.launch {
            navigate("trigger_setup_enable_pro_mode", NavDestination.ProMode)
        }
    }

    override fun onScreenOffTriggerSetupCheckedChange(isChecked: Boolean) {
        isScreenOffChecked.value = isChecked
    }

    override fun onDismissTriggerSetup() {
        currentSetupShortcut.value = null
    }

    override fun onTriggerSetupRecordClick() {
        val setupState = triggerSetupState.value ?: return

        val enableEvdevRecording = when (setupState) {
            is TriggerSetupState.Volume -> setupState.isScreenOffChecked
            is TriggerSetupState.Keyboard -> setupState.isScreenOffChecked
            is TriggerSetupState.Power -> true
            is TriggerSetupState.FingerprintGesture -> false
            is TriggerSetupState.Mouse -> true
            is TriggerSetupState.Other -> setupState.isScreenOffChecked
            is TriggerSetupState.Gamepad.Dpad -> false
            is TriggerSetupState.Gamepad.SimpleButtons -> setupState.isScreenOffChecked
            // Always enable pro mode recording to increase the chances of detecting
            // the key
            is TriggerSetupState.NotDetected -> true
        }

        viewModelScope.launch {
            val recordTriggerState = recordTriggerController.state.firstOrNull() ?: return@launch

            val result: KMResult<*> = when (recordTriggerState) {
                is RecordTriggerState.CountingDown -> {
                    recordTriggerController.stopRecording()
                }

                is RecordTriggerState.Completed,
                RecordTriggerState.Idle,
                    -> recordTriggerController.startRecording(
                    enableEvdevRecording,
                )
            }

            result.onSuccess {
                currentSetupShortcut.value = null
            }

            // Show dialog if the accessibility service is disabled or crashed
            if (result is AccessibilityServiceError) {
                showFixAccessibilityServiceDialog(result)
            }
        }
    }

    override fun onFingerprintGestureTypeSelected(type: FingerprintGestureType) {
        selectedFingerprintGestureType.value = type
    }

    override fun onAddFingerprintGestureClick() {
        configTriggerUseCase.addFingerprintGesture(selectedFingerprintGestureType.value)
        currentSetupShortcut.value = null
    }

    override fun onGamepadButtonTypeSelected(type: TriggerSetupState.Gamepad.Type) {
        selectedGamePadType.value = type
    }

    override fun onEnableImeClick() {
        viewModelScope.launch {
            setupInputMethodUseCase.enableInputMethod()
        }
    }

    override fun onChooseImeClick() {
        viewModelScope.launch {
            setupInputMethodUseCase.chooseInputMethod().onFailure {
                Timber.e("Failed to choose input method when setting up trigger. Error: $it")
            }
        }
    }
}

interface TriggerSetupDelegate {
    val triggerSetupState: StateFlow<TriggerSetupState?>
    fun showTriggerSetup(shortcut: TriggerSetupShortcut)
    fun onDismissTriggerSetup()
    fun onEnableAccessibilityServiceClick()
    fun onEnableProModeClick()
    fun onScreenOffTriggerSetupCheckedChange(isChecked: Boolean)
    fun onTriggerSetupRecordClick()
    fun onFingerprintGestureTypeSelected(type: FingerprintGestureType)
    fun onAddFingerprintGestureClick()
    fun onGamepadButtonTypeSelected(type: TriggerSetupState.Gamepad.Type)
    fun onEnableImeClick()
    fun onChooseImeClick()
}
