package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.util.firstBlocking

/**
 * Created by sds100 on 17/04/2021.
 */

class DetectConstraintsUseCaseImpl(
    private val accessibilityService: IAccessibilityService,
    private val mediaAdapter: MediaAdapter,
    private val devicesAdapter: DevicesAdapter,
    private val displayAdapter: DisplayAdapter
) : DetectConstraintsUseCase {

    override fun isSatisfied(constraintState: ConstraintState): Boolean {
        val snapshot = getSnapshot()
        when (constraintState.mode) {
            ConstraintMode.AND -> {
                return constraintState.constraints.all { snapshot.isSatisfied(it) }
            }
            ConstraintMode.OR -> {
                return constraintState.constraints.any { snapshot.isSatisfied(it) }
            }
        }
    }

    private fun getSnapshot(): ConstraintSnapshot {
        return ConstraintSnapshot(
            appInForeground = accessibilityService.rootNode?.packageName,
            connectedBluetoothDevices = devicesAdapter.connectedBluetoothDevices.value,
            orientation = displayAdapter.orientation,
            isScreenOn = displayAdapter.isScreenOn.firstBlocking(),
            appsPlayingMedia = mediaAdapter.getPackagesPlayingMedia()
        )
    }
}

interface DetectConstraintsUseCase {
    fun isSatisfied(constraintState: ConstraintState): Boolean
}