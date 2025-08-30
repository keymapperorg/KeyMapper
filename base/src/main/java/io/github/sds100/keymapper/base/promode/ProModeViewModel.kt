package io.github.sds100.keymapper.base.promode

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
            useCase.isRootGranted,
            useCase.shizukuSetupState,
            ::buildSetupState
        ).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

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

    fun onBackClick() {
        viewModelScope.launch {
            popBackStack()
        }
    }

    fun onSetupWithKeyMapperClick() {
        viewModelScope.launch {
            navigate("setup_pro_mode_with_key_mapper", NavDestination.ProModeSetup)
        }
    }

    private fun buildSetupState(
        isSystemBridgeConnected: Boolean,
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
    ) : ProModeState()

    data object Started : ProModeState()
}
