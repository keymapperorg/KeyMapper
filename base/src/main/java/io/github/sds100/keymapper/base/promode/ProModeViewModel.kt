package io.github.sds100.keymapper.base.promode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProModeViewModel @Inject constructor(
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
    val warningState: StateFlow<ProModeWarningState> =
        useCase.isWarningUnderstood
            .flatMapLatest { isUnderstood -> createWarningStateFlow(isUnderstood) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                ProModeWarningState.CountingDown(
                    WARNING_COUNT_DOWN_SECONDS,
                ),
            )

    val setupState: StateFlow<State<ProModeSetupState>> =
        combine(
            useCase.isSystemBridgeConnected,
            useCase.setupProgress,
            useCase.isRootGranted,
            useCase.shizukuSetupState,
            ::buildSetupState
        ).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private fun createWarningStateFlow(isUnderstood: Boolean): Flow<ProModeWarningState> =
        if (isUnderstood) {
            flowOf(ProModeWarningState.Understood)
        } else {
            flow {
                repeat(WARNING_COUNT_DOWN_SECONDS) {
                    emit(ProModeWarningState.CountingDown(WARNING_COUNT_DOWN_SECONDS - it))
                    delay(1000L)
                }

                emit(ProModeWarningState.Idle)
            }
        }

    fun onWarningButtonClick() {
        useCase.onUnderstoodWarning()
    }

    fun onStopServiceClick() {
        useCase.stopSystemBridge()
    }

    fun onRootButtonClick() {
        useCase.startSystemBridge()
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
                    useCase.startSystemBridge()
                }
            }
        }
    }

    private fun buildSetupState(
        isSystemBridgeConnected: Boolean,
        setupProgress: Float,
        isRootGranted: Boolean,
        shizukuSetupState: ShizukuSetupState
    ): State<ProModeSetupState> {
        if (isSystemBridgeConnected) {
            return State.Data(ProModeSetupState.Started)
        } else {
            return State.Data(
                ProModeSetupState.Stopped(
                    isRootGranted = isRootGranted,
                    shizukuSetupState = shizukuSetupState,
                    setupProgress = setupProgress
                )
            )
        }
    }
}

sealed class ProModeWarningState {
    data class CountingDown(val seconds: Int) : ProModeWarningState()
    data object Idle : ProModeWarningState()
    data object Understood : ProModeWarningState()
}

sealed class ProModeSetupState {
    data class Stopped(
        val isRootGranted: Boolean,
        val shizukuSetupState: ShizukuSetupState,
        val setupProgress: Float
    ) : ProModeSetupState()

    data object Started : ProModeSetupState()
}
