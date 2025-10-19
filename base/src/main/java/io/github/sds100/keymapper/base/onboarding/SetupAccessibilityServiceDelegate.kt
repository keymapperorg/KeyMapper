package io.github.sds100.keymapper.base.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.base.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.AccessibilityServiceError
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AccessibilityServiceDialog {
    data class EnableService(val isRestrictedSetting: Boolean) : AccessibilityServiceDialog()
    data object RestartService : AccessibilityServiceDialog()
    data object CantFindSettings : AccessibilityServiceDialog()
}

@Singleton
class SetupAccessibilityServiceDelegateImpl @Inject constructor(
    private val useCase: ControlAccessibilityServiceUseCase,
    resourceProvider: ResourceProvider,
) : SetupAccessibilityServiceDelegate,
    ResourceProvider by resourceProvider {

    var dialogState: AccessibilityServiceDialog? by mutableStateOf(null)

    override val accessibilityServiceState: Flow<AccessibilityServiceState> = useCase.serviceState

    override fun showFixAccessibilityServiceDialog(error: AccessibilityServiceError) {
        dialogState = when (error) {
            AccessibilityServiceError.Disabled -> {
                val isRestricted = useCase.isRestrictedSetting()
                AccessibilityServiceDialog.EnableService(isRestricted)
            }

            AccessibilityServiceError.Crashed -> AccessibilityServiceDialog.RestartService
        }
    }

    override fun showEnableAccessibilityServiceDialog() {
        val state = accessibilityServiceState.firstBlocking()

        if (state == AccessibilityServiceState.DISABLED) {
            val isRestricted = useCase.isRestrictedSetting()
            dialogState = AccessibilityServiceDialog.EnableService(isRestricted)

        } else if (state == AccessibilityServiceState.CRASHED) {
            dialogState = AccessibilityServiceDialog.RestartService
        }
    }

    fun onStartServiceClick() {
        if (!useCase.startService()) {
            dialogState = AccessibilityServiceDialog.CantFindSettings
        } else {
            dialogState = null
        }
    }

    fun onRestartServiceClick() {
        if (!useCase.restartService()) {
            dialogState = AccessibilityServiceDialog.CantFindSettings
        } else {
            dialogState = null
        }
    }

    fun onCancelClick() {
        dialogState = null
    }

    fun onIgnoreCrashedClick() {
        useCase.acknowledgeCrashed()
        dialogState = null
    }

    override fun showCantFindAccessibilitySettingsDialog() {
        dialogState = AccessibilityServiceDialog.CantFindSettings
    }
}

interface SetupAccessibilityServiceDelegate {
    val accessibilityServiceState: Flow<AccessibilityServiceState>

    fun showFixAccessibilityServiceDialog(error: AccessibilityServiceError)
    fun showEnableAccessibilityServiceDialog()
    fun showCantFindAccessibilitySettingsDialog()
}