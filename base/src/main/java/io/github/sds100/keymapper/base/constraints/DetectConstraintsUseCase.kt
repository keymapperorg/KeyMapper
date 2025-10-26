package io.github.sds100.keymapper.base.constraints

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.sds100.keymapper.base.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.foldable.FoldableAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.lock.LockScreenAdapter
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.phone.PhoneAdapter
import io.github.sds100.keymapper.system.power.PowerAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class DetectConstraintsUseCaseImpl @AssistedInject constructor(
    @Assisted
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
    private val foldableAdapter: FoldableAdapter,
) : DetectConstraintsUseCase {

    @AssistedFactory
    interface Factory {
        fun create(
            accessibilityService: IAccessibilityService,
        ): DetectConstraintsUseCaseImpl
    }

    override fun getSnapshot(): ConstraintSnapshot = LazyConstraintSnapshot(
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
        foldableAdapter,
    )

    override fun onDependencyChanged(dependency: ConstraintDependency): Flow<ConstraintDependency> {
        return when (dependency) {
            ConstraintDependency.FOREGROUND_APP -> accessibilityService.activeWindowPackage.map { dependency }
            ConstraintDependency.APP_PLAYING_MEDIA, ConstraintDependency.MEDIA_PLAYING ->
                merge(
                    mediaAdapter.getActiveMediaSessionPackagesFlow(),
                    mediaAdapter.getActiveAudioVolumeStreamsFlow(),
                ).map { dependency }

            ConstraintDependency.CONNECTED_BT_DEVICES -> devicesAdapter.connectedBluetoothDevices.map { dependency }
            ConstraintDependency.SCREEN_STATE -> displayAdapter.isScreenOn.map { dependency }
            ConstraintDependency.DISPLAY_ORIENTATION -> displayAdapter.orientation.map { dependency }
            ConstraintDependency.FLASHLIGHT_STATE -> merge(
                cameraAdapter.isFlashlightOnFlow(CameraLens.FRONT),
                cameraAdapter.isFlashlightOnFlow(CameraLens.BACK),
            ).map { dependency }

            ConstraintDependency.WIFI_SSID -> networkAdapter.connectedWifiSSIDFlow.map { dependency }
            ConstraintDependency.WIFI_STATE -> networkAdapter.isWifiEnabledFlow().map { dependency }
            ConstraintDependency.CHOSEN_IME -> inputMethodAdapter.chosenIme.map { dependency }
            ConstraintDependency.DEVICE_LOCKED_STATE ->
                lockScreenAdapter.isLockedFlow().map { dependency }

            ConstraintDependency.LOCK_SCREEN_SHOWING ->
                merge(
                    lockScreenAdapter.isLockScreenShowingFlow(),
                    accessibilityService.activeWindowPackage,
                ).map { dependency }

            ConstraintDependency.PHONE_STATE -> phoneAdapter.callStateFlow.map { dependency }
            ConstraintDependency.CHARGING_STATE -> powerAdapter.isCharging.map { dependency }
            ConstraintDependency.HINGE_STATE -> foldableAdapter.hingeState.map { dependency }
        }
    }
}

interface DetectConstraintsUseCase {
    fun getSnapshot(): ConstraintSnapshot
    fun onDependencyChanged(dependency: ConstraintDependency): Flow<ConstraintDependency>
}
