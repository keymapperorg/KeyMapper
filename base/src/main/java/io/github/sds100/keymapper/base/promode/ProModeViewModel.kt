package io.github.sds100.keymapper.base.promode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProModeViewModel @Inject constructor(
    private val useCase: ProModeSetupUseCase,
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
    val proModeWarningState: StateFlow<ProModeWarningState> =
        useCase.isWarningUnderstood.flatMapLatest { isUnderstood ->
            if (isUnderstood) {
                flowOf(ProModeWarningState.Understood)
            } else {
                flow<ProModeWarningState> {
                    repeat(WARNING_COUNT_DOWN_SECONDS) {
                        emit(ProModeWarningState.CountingDown(WARNING_COUNT_DOWN_SECONDS - it))
                        delay(1000L)
                    }

                    emit(ProModeWarningState.Idle)
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ProModeWarningState.CountingDown(
                WARNING_COUNT_DOWN_SECONDS,
            ),
        )

    fun onWarningButtonClick() {
        useCase.onUnderstoodWarning()
    }
}

sealed class ProModeWarningState {
    data class CountingDown(val seconds: Int) : ProModeWarningState()
    data object Idle : ProModeWarningState()
    data object Understood : ProModeWarningState()
}

data class ProModeSetupState(
    val isRootDetected: Boolean,
    val isShizukuDetected: Boolean,
)
