package io.github.sds100.keymapper.base.trigger

import android.os.Build
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ViewModelHelper
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalCoroutinesApi::class)
@ViewModelScoped
class TriggerSetupDelegateImpl @Inject constructor(
    @Named("viewmodel")
    val viewModelScope: CoroutineScope,
    val controlAccessibilityServiceUseCase: ControlAccessibilityServiceUseCase,
    val recordTriggerController: RecordTriggerController,
    val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
) : TriggerSetupDelegate,
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    private val currentDiscoverShortcut: MutableStateFlow<TriggerDiscoverShortcut?> =
        MutableStateFlow(null)

    private val isScreenOffChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)

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
        currentDiscoverShortcut.flatMapLatest { shortcut ->
            if (shortcut == null) {
                flowOf(null)
            } else {
                when (shortcut) {
                    TriggerDiscoverShortcut.VOLUME -> buildSetupVolumeTriggerFlow()
                    TriggerDiscoverShortcut.POWER -> buildSetupPowerTriggerFlow()
                    TriggerDiscoverShortcut.FINGERPRINT_GESTURE -> TODO()
                    TriggerDiscoverShortcut.KEYBOARD -> TODO()
                    TriggerDiscoverShortcut.MOUSE -> TODO()
                    TriggerDiscoverShortcut.GAMEPAD -> TODO()
                    TriggerDiscoverShortcut.OTHER -> TODO()

                    else -> throw UnsupportedOperationException("Unhandled shortcut: $shortcut")
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    override fun onDiscoverShortcutClick(shortcut: TriggerDiscoverShortcut) {
        currentDiscoverShortcut.value = shortcut
    }

    private fun buildSetupVolumeTriggerFlow(): Flow<TriggerSetupState> {
        return combine(
            controlAccessibilityServiceUseCase.serviceState,
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
                remapStatus = RemapStatus.SUPPORTED,
            )
        }
    }

    private fun buildSetupPowerTriggerFlow(): Flow<TriggerSetupState> {
        return combine(
            controlAccessibilityServiceUseCase.serviceState,
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

    override fun onEnableAccessibilityServiceClick() {
        viewModelScope.launch {
            val state = controlAccessibilityServiceUseCase.serviceState.first()

            if (state == AccessibilityServiceState.DISABLED) {
                ViewModelHelper.handleAccessibilityServiceStoppedDialog(
                    resourceProvider = this@TriggerSetupDelegateImpl,
                    dialogProvider = this@TriggerSetupDelegateImpl,
                    startService = controlAccessibilityServiceUseCase::startService,
                )
            } else if (state == AccessibilityServiceState.CRASHED) {
                ViewModelHelper.handleAccessibilityServiceCrashedDialog(
                    resourceProvider = this@TriggerSetupDelegateImpl,
                    dialogProvider = this@TriggerSetupDelegateImpl,
                    restartService = controlAccessibilityServiceUseCase::restartService,
                )
            }
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
        currentDiscoverShortcut.value = null
    }

    override fun onTriggerSetupRecordClick() {
        val setupState = triggerSetupState.value ?: return

        val enableEvdevRecording = when (setupState) {
            is TriggerSetupState.Volume -> setupState.isScreenOffChecked
            is TriggerSetupState.Power -> true
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
                currentDiscoverShortcut.value = null
            }

            // Show dialog if the accessibility service is disabled or crashed
            handleServiceEventResult(result)
        }
    }

    private suspend fun handleServiceEventResult(result: KMResult<*>) {
        if (result is KMError.AccessibilityServiceDisabled) {
            ViewModelHelper.handleAccessibilityServiceStoppedDialog(
                resourceProvider = this,
                dialogProvider = this,
                startService = controlAccessibilityServiceUseCase::startService,
            )
        }

        if (result is KMError.AccessibilityServiceCrashed) {
            ViewModelHelper.handleAccessibilityServiceCrashedDialog(
                resourceProvider = this,
                dialogProvider = this,
                restartService = controlAccessibilityServiceUseCase::restartService,
            )
        }
    }
}

interface TriggerSetupDelegate {
    val triggerSetupState: StateFlow<TriggerSetupState?>
    fun onDiscoverShortcutClick(shortcut: TriggerDiscoverShortcut)
    fun onDismissTriggerSetup()
    fun onEnableAccessibilityServiceClick()
    fun onEnableProModeClick()
    fun onScreenOffTriggerSetupCheckedChange(isChecked: Boolean)
    fun onTriggerSetupRecordClick()
}
