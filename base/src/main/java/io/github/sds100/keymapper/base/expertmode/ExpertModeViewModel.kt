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

    private val startingMethod =
        MutableStateFlow<SystemBridgeStartMethod?>(null)

    init {
        viewModelScope.launch {
            useCase.isSystemBridgeConnected.collect { connected ->
                if (connected) {
                    startingMethod.value = null
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val warningState: StateFlow<ExpertModeWarningState> =
        useCase.isWarningUnderstood
            .flatMapLatest { isUnderstood -> createWarningStateFlow(isUnderstood) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                ExpertModeWarningState.CountingDown(WARNING_COUNT_DOWN_SECONDS),
            )

    val setupState: StateFlow<State<ExpertModeState>> =
        combine(
            useCase.isSystemBridgeConnected,
            useCase.isRootGranted,
            useCase.shizukuSetupState,
            useCase.isNotificationPermissionGranted,
            startingMethod,
            useCase.isSystemBridgeStarting,
        ) { connected, rootGranted, shizukuState, notifGranted, method, isStarting ->
            buildSetupState(
                connected,
                rootGranted,
                shizukuState,
                notifGranted,
                method,
                isStarting,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            State.Loading,
        )

    val autoStartBootEnabled: StateFlow<Boolean> =
        useCase.isAutoStartBootEnabled
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    var showInfoCard by mutableStateOf(!useCase.isInfoDismissed())
        private set

    fun hideInfoCard() {
        showInfoCard = false
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
                    emit(
                        ExpertModeWarningState.CountingDown(
                            WARNING_COUNT_DOWN_SECONDS - it,
                        ),
                    )
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
        startingMethod.value = SystemBridgeStartMethod.ROOT
        useCase.startSystemBridgeWithRoot()
    }

    fun onShizukuButtonClick() {
        viewModelScope.launch {
            when (useCase.shizukuSetupState.first()) {
                ShizukuSetupState.NOT_FOUND -> Unit
                ShizukuSetupState.INSTALLED -> useCase.openShizukuApp()
                ShizukuSetupState.STARTED -> useCase.requestShizukuPermission()
                ShizukuSetupState.PERMISSION_GRANTED -> {
                    startingMethod.value = SystemBridgeStartMethod.SHIZUKU
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
            navigate(
                "setup_expert_mode_with_key_mapper",
                NavDestination.ExpertModeSetup,
            )
        }
    }

    fun onRequestNotificationPermissionClick() {
        useCase.requestNotificationPermission()
    }

    fun onAutoStartBootToggled() {
        useCase.toggleAutoStartBoot()
    }

    private fun buildSetupState(
        isSystemBridgeConnected: Boolean,
        isRootGranted: Boolean,
        shizukuSetupState: ShizukuSetupState,
        isNotificationPermissionGranted: Boolean,
        startingMethod: SystemBridgeStartMethod?,
        isSystemBridgeStarting: Boolean,
    ): State<ExpertModeState> {
        return if (isSystemBridgeConnected) {
            State.Data(
                ExpertModeState.Started(
                    isDefaultUsbModeCompatible =
                        useCase.isCompatibleUsbModeSelected().valueOrNull() ?: false,
                ),
            )
        } else {
            State.Data(
                ExpertModeState.Stopped(
                    isRootGranted = isRootGranted,
                    shizukuSetupState = shizukuSetupState,
                    isNotificationPermissionGranted = isNotificationPermissionGranted,
                    startingMethod = startingMethod,
                    isStarting = startingMethod != null || isSystemBridgeStarting,
                ),
            )
        }
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
        val startingMethod: SystemBridgeStartMethod?,
        val isStarting: Boolean,
    ) : ExpertModeState()

    data class Started(
        val isDefaultUsbModeCompatible: Boolean,
    ) : ExpertModeState()
}
