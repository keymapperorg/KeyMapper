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

    val setupState: StateFlow<State<ProModeState>> =
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

    fun onSetupWithKeyMapperClick() {
        // TODO Settings screen will be refactored into compose and so NavigationProvider will work
    }

    private fun buildSetupState(
        isSystemBridgeConnected: Boolean,
        setupProgress: Float,
        isRootGranted: Boolean,
        shizukuSetupState: ShizukuSetupState
    ): State<ProModeState> {
        if (isSystemBridgeConnected) {
            return State.Data(ProModeState.Started)
        } else {
            return State.Data(
                ProModeState.Stopped(
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

sealed class ProModeState {
    data class Stopped(
        val isRootGranted: Boolean,
        val shizukuSetupState: ShizukuSetupState,
        val setupProgress: Float
    ) : ProModeState()

    data object Started : ProModeState()
}
