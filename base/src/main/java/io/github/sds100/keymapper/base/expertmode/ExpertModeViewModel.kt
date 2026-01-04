package io.github.sds100.keymapper.base.expertmode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.valueOrNull
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ExpertModeViewModel @Inject constructor(
    private val useCase: SystemBridgeSetupUseCase,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    companion object {
        private const val WARNING_COUNT_DOWN_SECONDS = 5
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val warningState: StateFlow<ExpertModeWarningState> =
        useCase.isWarningUnderstood
            .flatMapLatest { isUnderstood -> createWarningStateFlow(isUnderstood) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                ExpertModeWarningState.CountingDown(
                    WARNING_COUNT_DOWN_SECONDS,
                ),
            )

    private val shellStartCommandState: MutableStateFlow<ShellStartCommandState> =
        MutableStateFlow(ShellStartCommandState.Idle)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<State<ExpertModeState>> =
        useCase.isSystemBridgeConnected.flatMapLatest { isSystemBridgeConnected ->
            if (isSystemBridgeConnected) {
                startedStateFlow().map { State.Data(it) }
            } else {
                stoppedStateFlow().map { State.Data(it) }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    var showInfoCard by mutableStateOf(!useCase.isInfoDismissed())
        private set

    fun hideInfoCard() {
        showInfoCard = false
        // Save that they've dismissed the card so it is not shown by default the next
        // time they visit the PRO mode page.
        useCase.dismissInfo()
    }

    fun showInfoCard() {
        showInfoCard = true
    }

    private fun createWarningStateFlow(isUnderstood: Boolean): Flow<ExpertModeWarningState> =
        if (isUnderstood) {
            flowOf(ExpertModeWarningState.Understood)
        } else {
            flow {
                repeat(WARNING_COUNT_DOWN_SECONDS) {
                    emit(ExpertModeWarningState.CountingDown(WARNING_COUNT_DOWN_SECONDS - it))
                    delay(1000L)
                }

                emit(ExpertModeWarningState.Idle)
            }
        }

    fun onWarningButtonClick() {
        useCase.onUnderstoodWarning()
    }

    fun onStopServiceClick() {
        useCase.stopSystemBridge()
    }

    fun onRootButtonClick() {
        useCase.startSystemBridgeWithRoot()
    }

    fun onShizukuButtonClick() {
        viewModelScope.launch {
            val shizukuState = useCase.shizukuSetupState.first()
            when (shizukuState) {
                ShizukuSetupState.NOT_FOUND -> {
                    // Do nothing
                }

                ShizukuSetupState.INSTALLED -> {
                    useCase.openShizukuApp()
                }

                ShizukuSetupState.STARTED -> {
                    useCase.requestShizukuPermission()
                }

                ShizukuSetupState.PERMISSION_GRANTED -> {
                    useCase.startSystemBridgeWithShizuku()
                }
            }
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            popBackStack()
        }
    }

    fun onSetupWithKeyMapperClick() {
        viewModelScope.launch {
            navigate("setup_expert_mode_with_key_mapper", NavDestination.ExpertModeSetup)
        }
    }

    fun onRequestNotificationPermissionClick() {
        useCase.requestNotificationPermission()
    }

    fun onAutoStartBootToggled() {
        useCase.toggleAutoStartBoot()
    }

    fun onLaunchDeveloperOptionsClick() {
        useCase.launchDeveloperOptions()
    }

    fun onGetShellStartCommandClick() {
        viewModelScope.launch {
            if (shellStartCommandState.value != ShellStartCommandState.Idle) {
                // Do not get the command if not idle.
                return@launch
            }

            shellStartCommandState.value = ShellStartCommandState.Loading
            val result = useCase.getShellStartCommand()
            shellStartCommandState.value = when (result) {
                is Success -> ShellStartCommandState.Loaded(result.value)
                else -> ShellStartCommandState.Error
            }
        }
    }

    private fun stoppedStateFlow(): Flow<ExpertModeState.Stopped> = combine(
        useCase.isRootGranted,
        useCase.shizukuSetupState,
        useCase.isNotificationPermissionGranted,
        useCase.isSystemBridgeStarting,
        shellStartCommandState,
    ) {
            isRootGranted,
            shizukuSetupState,
            isNotificationPermissionGranted,
            isSystemBridgeStarting,
            shellStartCommandState,
        ->
        ExpertModeState.Stopped(
            isRootGranted = isRootGranted,
            shizukuSetupState = shizukuSetupState,
            isNotificationPermissionGranted = isNotificationPermissionGranted,
            isStarting = isSystemBridgeStarting,
            shellStartCommandState = shellStartCommandState,
        )
    }

    private fun startedStateFlow(): Flow<ExpertModeState.Started> = combine(
        useCase.isAutoStartBootEnabled,
        useCase.isAutoStartBootAllowed,
        useCase.isAdbInputSecurityEnabled,
    ) { autoStartBootChecked, autoStartBootEnabled, isAdbInputSecurityEnabled ->
        ExpertModeState.Started(
            isDefaultUsbModeCompatible =
            useCase.isCompatibleUsbModeSelected().valueOrNull()
                ?: false,
            autoStartBootChecked = autoStartBootChecked,
            autoStartBootEnabled = autoStartBootEnabled,
            isAdbInputSecurityEnabled = isAdbInputSecurityEnabled,
        )
    }
}

sealed class ExpertModeWarningState {
    data class CountingDown(val seconds: Int) : ExpertModeWarningState()
    data object Idle : ExpertModeWarningState()
    data object Understood : ExpertModeWarningState()
}

sealed class ExpertModeState {
    data class Stopped(
        val isRootGranted: Boolean,
        val shizukuSetupState: ShizukuSetupState,
        val isNotificationPermissionGranted: Boolean,
        val isStarting: Boolean,
        val shellStartCommandState: ShellStartCommandState,
    ) : ExpertModeState()

    data class Started(
        val isDefaultUsbModeCompatible: Boolean,
        val autoStartBootChecked: Boolean,
        val autoStartBootEnabled: Boolean,
        val isAdbInputSecurityEnabled: Boolean?,
    ) : ExpertModeState()
}

sealed class ShellStartCommandState {
    data object Idle : ShellStartCommandState()
    data object Loading : ShellStartCommandState()
    data class Loaded(val command: String) : ShellStartCommandState()
    data object Error : ShellStartCommandState()
}
