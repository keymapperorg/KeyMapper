package io.github.sds100.keymapper.common.utils

/**
 * Represents the physical orientation of the device based on the device's
 * orientation sensor (accelerometer), independent of the screen rotation setting.
 */
enum class PhysicalOrientation {
    PORTRAIT,
    LANDSCAPE,
    PORTRAIT_INVERTED,
    LANDSCAPE_INVERTED,
}
