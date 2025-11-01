package io.github.sds100.keymapper.system.foldable

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the state of a foldable device hinge.
 */
sealed class HingeState {
    /**
     * Hinge sensor is not available on this device.
     */
    data object Unavailable : HingeState()

    /**
     * Hinge sensor is available and reporting angle.
     * @param angle The angle in degrees of the hinge.
     * 0 degrees means the device is closed/flat.
     * 180 degrees means the device is fully open.
     */
    data class Available(val angle: Float) : HingeState()
}

fun HingeState.Available.isOpen(): Boolean = angle >= 150
fun HingeState.Available.isClosed(): Boolean = angle < 30

@RequiresApi(Build.VERSION_CODES.R)
interface FoldableAdapter {
    /**
     * StateFlow that emits the current hinge state.
     */
    val hingeState: StateFlow<HingeState>
}
