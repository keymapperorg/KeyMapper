package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.models.EvdevDeviceInfo

/**
 * Identifies which device triggered the action.
 */
sealed class PerformActionTriggerDevice {
    /**
     * The action was triggered by an evdev-level input device.
     */
    data class Evdev(val device: EvdevDeviceInfo) : PerformActionTriggerDevice()

    data object Default : PerformActionTriggerDevice()
}
