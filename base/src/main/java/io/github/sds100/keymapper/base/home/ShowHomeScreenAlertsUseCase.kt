package io.github.sds100.keymapper.base.home

import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ShowHomeScreenAlertsUseCaseImpl @Inject constructor(
    private val preferences: PreferenceRepository,
    private val permissions: PermissionAdapter,
    private val accessibilityServiceAdapter: AccessibilityServiceAdapter,
    private val pauseKeyMapsUseCase: PauseKeyMapsUseCase,
) : ShowHomeScreenAlertsUseCase {
    override val hideAlerts: Flow<Boolean> =
        preferences.get(Keys.hideHomeScreenAlerts).map { it == true }

    override val isBatteryOptimised: Flow<Boolean> =
        permissions.isGrantedFlow(Permission.IGNORE_BATTERY_OPTIMISATION)
            .map { !it } // if granted then battery is NOT optimised

    override val areKeyMapsPaused: Flow<Boolean> = pauseKeyMapsUseCase.isPaused

    override val isLoggingEnabled: Flow<Boolean> = preferences.get(Keys.log).map { it == true }

    override val accessibilityServiceState: Flow<AccessibilityServiceState> =
        accessibilityServiceAdapter.state

    override fun disableBatteryOptimisation() {
        permissions.request(Permission.IGNORE_BATTERY_OPTIMISATION)
    }

    override fun startAccessibilityService(): Boolean = accessibilityServiceAdapter.start()

    override fun restartAccessibilityService(): Boolean = accessibilityServiceAdapter.restart()

    override fun acknowledgeCrashed() {
        accessibilityServiceAdapter.acknowledgeCrashed()
    }

    override fun resumeMappings() {
        pauseKeyMapsUseCase.resume()
    }

    override fun disableLogging() {
        preferences.set(Keys.log, false)
    }

    override val showNotificationPermissionAlert: Flow<Boolean> =
        combine(
            permissions.isGrantedFlow(Permission.POST_NOTIFICATIONS),
            preferences.get(Keys.neverShowNotificationPermissionAlert).map { it ?: false },
        ) { isGranted, neverShow ->
            !isGranted && !neverShow
        }

    override fun requestNotificationPermission() {
        permissions.request(Permission.POST_NOTIFICATIONS)
    }

    override fun neverShowNotificationPermissionAlert() {
        preferences.set(Keys.neverShowNotificationPermissionAlert, true)
    }
}

interface ShowHomeScreenAlertsUseCase {
    val accessibilityServiceState: Flow<AccessibilityServiceState>
    fun startAccessibilityService(): Boolean
    fun restartAccessibilityService(): Boolean
    fun acknowledgeCrashed()

    val hideAlerts: Flow<Boolean>
    fun disableBatteryOptimisation()
    val isBatteryOptimised: Flow<Boolean>
    val areKeyMapsPaused: Flow<Boolean>
    fun resumeMappings()

    val isLoggingEnabled: Flow<Boolean>
    fun disableLogging()

    val showNotificationPermissionAlert: Flow<Boolean>
    fun requestNotificationPermission()
    fun neverShowNotificationPermissionAlert()
}
