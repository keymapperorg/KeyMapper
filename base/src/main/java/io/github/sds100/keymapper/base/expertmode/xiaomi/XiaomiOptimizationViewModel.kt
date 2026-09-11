package io.github.sds100.keymapper.base.expertmode.xiaomi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class XiaomiOptimizationViewModel @Inject constructor(
    private val useCase: XiaomiOptimizationUseCase,
    resourceProvider: ResourceProvider,
    navigationProvider: NavigationProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    NavigationProvider by navigationProvider {

    private val isApplyingBatteryFixes = MutableStateFlow(false)
    private val isTogglingMiuiOptimization = MutableStateFlow(false)

    val uiState: StateFlow<XiaomiOptimizationUiState> = combine(
        useCase.isMiuiOptimizationDisabled,
        useCase.isBatteryFixesApplied,
        isApplyingBatteryFixes,
        isTogglingMiuiOptimization,
    ) {
            isMiuiOptimizationDisabled,
            isBatteryFixesApplied,
            isApplyingBatteryFixes,
            isTogglingMiuiOptimization,
        ->
        XiaomiOptimizationUiState(
            isMiuiOptimizationDisabled = isMiuiOptimizationDisabled,
            isBatteryFixesApplied = isBatteryFixesApplied,
            isApplyingBatteryFixes = isApplyingBatteryFixes,
            isTogglingMiuiOptimization = isTogglingMiuiOptimization,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        XiaomiOptimizationUiState(),
    )

    var userMessage: String? by mutableStateOf(null)
        private set

    fun onBackClick() {
        viewModelScope.launch {
            popBackStack()
        }
    }

    fun onApplyBatteryFixesClick() {
        viewModelScope.launch {
            isApplyingBatteryFixes.value = true

            val success = useCase.applyBatteryFixes()

            if (!success) {
                userMessage = getString(R.string.xiaomi_optimization_apply_error)
            }

            isApplyingBatteryFixes.value = false
        }
    }

    fun onMiuiOptimizationChange(disable: Boolean) {
        viewModelScope.launch {
            isTogglingMiuiOptimization.value = true

            val success = useCase.setMiuiOptimizationDisabled(disable)

            userMessage = if (success) {
                getString(R.string.xiaomi_optimization_advanced_reboot_note)
            } else {
                getString(R.string.xiaomi_optimization_apply_error)
            }

            isTogglingMiuiOptimization.value = false
        }
    }

    fun onOpenAutostartClick() {
        if (!useCase.openAutostartSettings()) {
            userMessage = getString(R.string.xiaomi_optimization_open_settings_error)
        }
    }

    fun onOpenBatterySaverClick() {
        if (!useCase.openBatterySaverSettings()) {
            userMessage = getString(R.string.xiaomi_optimization_open_settings_error)
        }
    }

    fun onUserMessageShown() {
        userMessage = null
    }
}

data class XiaomiOptimizationUiState(
    val isMiuiOptimizationDisabled: Boolean? = null,
    val isBatteryFixesApplied: Boolean? = null,
    val isApplyingBatteryFixes: Boolean = false,
    val isTogglingMiuiOptimization: Boolean = false,
)
