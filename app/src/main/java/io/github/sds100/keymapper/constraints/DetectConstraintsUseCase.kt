package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.lock.LockScreenAdapter
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.phone.PhoneAdapter
import io.github.sds100.keymapper.system.power.PowerAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 17/04/2021.
 */

class DetectConstraintsUseCaseImpl(
    private val accessibilityService: IAccessibilityService,
    private val mediaAdapter: MediaAdapter,
    private val devicesAdapter: DevicesAdapter,
    private val displayAdapter: DisplayAdapter,
    private val cameraAdapter: CameraAdapter,
    private val networkAdapter: NetworkAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val lockScreenAdapter: LockScreenAdapter,
    private val phoneAdapter: PhoneAdapter,
    private val powerAdapter: PowerAdapter,
) : DetectConstraintsUseCase {

    override fun getSnapshot(): ConstraintSnapshotImpl = ConstraintSnapshotImpl(
        accessibilityService,
        mediaAdapter,
        devicesAdapter,
        displayAdapter,
        networkAdapter,
        cameraAdapter,
        inputMethodAdapter,
        lockScreenAdapter,
        phoneAdapter,
        powerAdapter,
    )

    override fun onDependencyChanged(dependency: ConstraintDependency): Flow<ConstraintDependency> {
        when (dependency) {
            ConstraintDependency.FOREGROUND_APP -> return accessibilityService.activeWindowPackage.map { dependency }
            ConstraintDependency.APP_PLAYING_MEDIA -> TODO()
            ConstraintDependency.MEDIA_PLAYING -> TODO()
            ConstraintDependency.CONNECTED_BT_DEVICES -> return devicesAdapter.connectedBluetoothDevices.map { dependency }
            ConstraintDependency.SCREEN_STATE -> return displayAdapter.isScreenOn.map { dependency }
            ConstraintDependency.DISPLAY_ORIENTATION -> TODO()
            ConstraintDependency.FLASHLIGHT_STATE -> TODO()
            ConstraintDependency.WIFI_SSID -> TODO()
            ConstraintDependency.WIFI_STATE -> TODO()
            ConstraintDependency.CHOSEN_IME -> return inputMethodAdapter.chosenIme.map { dependency }
            ConstraintDependency.DEVICE_LOCKED_STATE -> TODO()
            ConstraintDependency.PHONE_STATE -> return phoneAdapter.callStateFlow.map { dependency }
            ConstraintDependency.CHARGING_STATE -> return powerAdapter.isCharging.map { dependency }
        }
    }
}

interface DetectConstraintsUseCase {
    fun getSnapshot(): ConstraintSnapshot
    fun onDependencyChanged(dependency: ConstraintDependency): Flow<ConstraintDependency>
}
