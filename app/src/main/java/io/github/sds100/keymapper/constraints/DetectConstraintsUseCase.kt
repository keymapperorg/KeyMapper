package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.media.MediaAdapter

/**
 * Created by sds100 on 17/04/2021.
 */

class DetectConstraintsUseCaseImpl(
    private val accessibilityService: IAccessibilityService,
    private val mediaAdapter: MediaAdapter,
    private val devicesAdapter: DevicesAdapter,
    private val displayAdapter: DisplayAdapter
) : DetectConstraintsUseCase {

    override fun getSnapshot(): ConstraintSnapshot {
        return ConstraintSnapshot(accessibilityService, mediaAdapter, devicesAdapter, displayAdapter)
    }
}

interface DetectConstraintsUseCase {
    fun getSnapshot(): ConstraintSnapshot
}