package io.github.sds100.keymapper.util.delegate

import io.github.sds100.keymapper.data.model.FingerprintMap
import io.github.sds100.keymapper.util.FingerprintMapUtils
import io.github.sds100.keymapper.util.IActionError
import io.github.sds100.keymapper.util.IConstraintDelegate
import kotlinx.coroutines.CoroutineScope
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 11/12/20.
 */
class FingerprintGestureMapController(
    coroutineScope: CoroutineScope,
    iConstraintDelegate: IConstraintDelegate,
    iActionError: IActionError
) : SimpleMappingController(coroutineScope, iConstraintDelegate, iActionError) {

    var fingerprintMaps: Map<String, FingerprintMap> = emptyMap()
        set(value) {
            reset()

            field = value
        }

    fun onGesture(sdkGestureId: Int) {
        val keyMapperId = FingerprintMapUtils.SDK_ID_TO_KEY_MAPPER_ID[sdkGestureId] ?: return

        fingerprintMaps[keyMapperId]?.apply {
            onDetected(
                keyMapperId,
                actionList,
                constraintList,
                constraintMode,
                isEnabled,
                extras,
                flags.hasFlag(FingerprintMap.FLAG_VIBRATE),
                flags.hasFlag(FingerprintMap.FLAG_SHOW_TOAST)
            )
        }
    }
}