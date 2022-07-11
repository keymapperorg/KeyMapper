package io.github.sds100.keymapper.onboarding

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.fingerprintmaps.AreFingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.shizuku.ShizukuAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Created by sds100 on 14/04/2021.
 */
class AppIntroUseCaseImpl @Inject constructor(
    private val permissionAdapter: PermissionAdapter,
    private val accessibilityServiceAdapter: AccessibilityServiceAdapter,
    private val preferenceRepository: PreferenceRepository,
    private val fingerprintGesturesSupportedUseCase: AreFingerprintGesturesSupportedUseCase,
    private val shizukuAdapter: ShizukuAdapter
) : AppIntroUseCase {
    override val serviceState: Flow<ServiceState> = accessibilityServiceAdapter.state

    override val isBatteryOptimised: Flow<Boolean> =
        permissionAdapter.isGrantedFlow(Permission.IGNORE_BATTERY_OPTIMISATION).map { !it }

    override val fingerprintGesturesSupported: Flow<Boolean?> =
        fingerprintGesturesSupportedUseCase.isSupported

    override val isShizukuPermissionGranted: Flow<Boolean> = permissionAdapter.isGrantedFlow(Permission.SHIZUKU)

    override val isShizukuStarted: Boolean
        get() = shizukuAdapter.isStarted.value

    override fun ignoreBatteryOptimisation() {
        permissionAdapter.request(Permission.IGNORE_BATTERY_OPTIMISATION)
    }

    override fun enableAccessibilityService() {
        accessibilityServiceAdapter.start()
    }

    override fun restartAccessibilityService() {
        accessibilityServiceAdapter.restart()
    }

    override fun shownAppIntro() {
        preferenceRepository.set(Keys.approvedFingerprintFeaturePrompt, true)
        preferenceRepository.set(Keys.approvedSetupChosenDevicesAgain, true)
        preferenceRepository.set(Keys.shownAppIntro, true)
    }

    override fun openShizuku() {
        shizukuAdapter.openShizukuApp()
    }

    override fun requestShizukuPermission() {
        permissionAdapter.request(Permission.SHIZUKU)
    }

    override fun shownShizukuPermissionPrompt() {
        preferenceRepository.set(Keys.shownShizukuPermissionPrompt, true)
    }
}

interface AppIntroUseCase {
    val serviceState: Flow<ServiceState>
    val isBatteryOptimised: Flow<Boolean>
    val fingerprintGesturesSupported: Flow<Boolean?>
    val isShizukuPermissionGranted: Flow<Boolean>
    val isShizukuStarted: Boolean

    fun ignoreBatteryOptimisation()
    fun enableAccessibilityService()
    fun restartAccessibilityService()
    fun requestShizukuPermission()
    fun shownShizukuPermissionPrompt()
    fun openShizuku()

    fun shownAppIntro()
}