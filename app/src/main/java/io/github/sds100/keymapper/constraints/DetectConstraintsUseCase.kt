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
    private val phoneAdapter: PhoneAdapter
) : DetectConstraintsUseCase {

    override fun getSnapshot(): ConstraintSnapshotImpl {
        return ConstraintSnapshotImpl(
            accessibilityService,
            mediaAdapter,
            devicesAdapter,
            displayAdapter,
            networkAdapter,
            cameraAdapter,
            inputMethodAdapter,
            lockScreenAdapter,
            phoneAdapter
        )
    }
}

interface DetectConstraintsUseCase {
    fun getSnapshot(): ConstraintSnapshot
}