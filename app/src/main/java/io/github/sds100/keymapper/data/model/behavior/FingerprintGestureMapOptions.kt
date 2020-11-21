package io.github.sds100.keymapper.data.model.behavior

import io.github.sds100.keymapper.data.model.FingerprintGestureMap
import io.github.sds100.keymapper.data.model.behavior.BehaviorOption.Companion.applyBehaviorOption
import io.github.sds100.keymapper.data.model.getData
import io.github.sds100.keymapper.util.result.valueOrNull
import splitties.bitflags.hasFlag
import java.io.Serializable

/**
 * Created by sds100 on 18/11/20.
 */

class FingerprintGestureMapOptions(val gestureId: String, fingerprintGestureMap: FingerprintGestureMap) : Serializable {
    companion object {
        const val ID_VIBRATE = "vibrate"
        const val ID_VIBRATION_DURATION = "vibration_duration"
    }

    val vibrate = BehaviorOption(
        id = ID_VIBRATE,
        value = fingerprintGestureMap.flags.hasFlag(FingerprintGestureMap.FLAG_VIBRATE),
        isAllowed = true
    )

    val vibrateDuration: BehaviorOption<Int>

    init {
        val vibrateDurationValue =
            fingerprintGestureMap.extras.getData(FingerprintGestureMap.EXTRA_VIBRATION_DURATION).valueOrNull()?.toInt()

        vibrateDuration = BehaviorOption(
            id = ID_VIBRATION_DURATION,
            value = vibrateDurationValue ?: BehaviorOption.DEFAULT,
            isAllowed = vibrate.value
        )

    }

    fun setValue(id: String, value: Boolean): FingerprintGestureMapOptions {
        when (id) {
            ID_VIBRATE -> {
                vibrate.value = value
                vibrateDuration.isAllowed = value
            }
        }

        return this
    }

    fun setValue(id: String, value: Int): FingerprintGestureMapOptions {
        when (id) {
            ID_VIBRATION_DURATION -> vibrateDuration.value = value
        }

        return this
    }

    fun applyToFingerprintGestureMap(fingerprintGestureMap: FingerprintGestureMap): FingerprintGestureMap {
        val newFlags = fingerprintGestureMap.flags
            .applyBehaviorOption(vibrate, FingerprintGestureMap.FLAG_VIBRATE)

        val newExtras = fingerprintGestureMap.extras
            .applyBehaviorOption(vibrateDuration, FingerprintGestureMap.EXTRA_VIBRATION_DURATION)

        return fingerprintGestureMap.copy(flags = newFlags, extras = newExtras)
    }
}