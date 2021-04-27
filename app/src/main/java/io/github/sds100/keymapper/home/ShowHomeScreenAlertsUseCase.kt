package io.github.sds100.keymapper.home

import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 04/04/2021.
 */

class ShowHomeScreenAlertsUseCaseImpl(
    private val preferences: PreferenceRepository,
    private val permissions: PermissionAdapter,
    private val controlService: ControlAccessibilityServiceUseCase
) : ShowHomeScreenAlertsUseCase {
    override val hideAlerts: Flow<Boolean> =
        preferences.get(Keys.hideHomeScreenAlerts).map { it ?: false }

    override val isBatteryOptimised: Flow<Boolean> = channelFlow {
        send(!permissions.isGranted(Permission.IGNORE_BATTERY_OPTIMISATION))

        permissions.onPermissionsUpdate.collectLatest {
            send(!permissions.isGranted(Permission.IGNORE_BATTERY_OPTIMISATION))
        }
    }

    override fun disableBatteryOptimisation() {
        permissions.request(Permission.IGNORE_BATTERY_OPTIMISATION)
    }

    override val isAccessibilityServiceEnabled: Flow<Boolean> = controlService.isEnabled
    override fun enableAccessibilityService() {
        controlService.enable()
    }
}

interface ShowHomeScreenAlertsUseCase{
    val isAccessibilityServiceEnabled: Flow<Boolean>
    fun enableAccessibilityService()

    val hideAlerts: Flow<Boolean>
    fun disableBatteryOptimisation()
    val isBatteryOptimised: Flow<Boolean>
}