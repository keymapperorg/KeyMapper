package io.github.sds100.keymapper.base.expertmode

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Pushes system bridge configuration preferences down to the SystemBridge process whenever
 * they change or the bridge (re)connects. Currently this is only the power button emergency
 * stop toggle (see issue #2203).
 */
@Singleton
class SystemBridgeConfigSync @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val systemBridgeConnManager: SystemBridgeConnectionManager,
    private val preferenceRepository: PreferenceRepository,
) {

    private val emergencyStopEnabled: StateFlow<Boolean> =
        preferenceRepository.get(Keys.isSystemBridgeEmergencyStopEnabled)
            .map { it ?: PreferenceDefaults.SYSTEM_BRIDGE_EMERGENCY_STOP_ENABLED }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                PreferenceDefaults.SYSTEM_BRIDGE_EMERGENCY_STOP_ENABLED,
            )

    fun start() {
        // Re-apply the config whenever the bridge connects (e.g. after a restart).
        coroutineScope.launch {
            systemBridgeConnManager.connectionState
                .filterIsInstance<SystemBridgeConnectionState.Connected>()
                .collect {
                    pushEmergencyStopEnabled(emergencyStopEnabled.value)
                }
        }

        // Apply the config immediately whenever the preference changes.
        coroutineScope.launch {
            emergencyStopEnabled.collect { enabled ->
                pushEmergencyStopEnabled(enabled)
            }
        }
    }

    private fun pushEmergencyStopEnabled(enabled: Boolean) {
        systemBridgeConnManager.run { bridge ->
            bridge.setEmergencyStopEnabled(enabled)
        }
    }
}
