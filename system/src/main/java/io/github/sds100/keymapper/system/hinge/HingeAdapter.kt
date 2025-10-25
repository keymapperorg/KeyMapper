package io.github.sds100.keymapper.system.hinge

import kotlinx.coroutines.flow.Flow

/**
 * Represents the state of a foldable device hinge.
 */
data class HingeState(
    /**
     * True if the device has a hinge and it is currently available.
     */
    val isAvailable: Boolean,
    /**
     * The angle in degrees of the hinge. Null if hinge is not available.
     * 0 degrees means the device is closed/flat.
     * 180 degrees means the device is fully open.
     */
    val angle: Float?,
)

interface HingeAdapter {
    /**
     * Flow that emits the current hinge state.
     */
    val hingeState: Flow<HingeState>

    /**
     * Get the current cached hinge state.
     */
    fun getCachedHingeState(): HingeState
}
