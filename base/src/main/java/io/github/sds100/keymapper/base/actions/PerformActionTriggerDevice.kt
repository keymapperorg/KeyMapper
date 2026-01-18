package io.github.sds100.keymapper.base.actions

/**
 * Identifies which device triggered the action.
 */
sealed class PerformActionTriggerDevice {
    /**
     * The action was triggered by an evdev-level input device.
     */
    data class Evdev(val deviceId: Int) : PerformActionTriggerDevice()

    /**
     * The action was triggered by an Android InputDevice.
     */
    data class AndroidDevice(val deviceId: Int) : PerformActionTriggerDevice()

    data object Default : PerformActionTriggerDevice()
}
