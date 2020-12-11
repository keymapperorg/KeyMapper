package io.github.sds100.keymapper.util

import android.content.Context
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.DataStoreKeys
import io.github.sds100.keymapper.data.model.FingerprintMap
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 14/11/20.
 */
object FingerprintGestureUtils {
    const val SWIPE_DOWN = "swipe_down"
    const val SWIPE_UP = "swipe_up"
    const val SWIPE_LEFT = "swipe_left"
    const val SWIPE_RIGHT = "swipe_right"

    val GESTURES = arrayOf(SWIPE_DOWN, SWIPE_UP, SWIPE_LEFT, SWIPE_RIGHT)

    val PREF_KEYS = mapOf(
        SWIPE_DOWN to DataStoreKeys.FINGERPRINT_GESTURE_SWIPE_DOWN,
        SWIPE_UP to DataStoreKeys.FINGERPRINT_GESTURE_SWIPE_UP,
        SWIPE_LEFT to DataStoreKeys.FINGERPRINT_GESTURE_SWIPE_LEFT,
        SWIPE_RIGHT to DataStoreKeys.FINGERPRINT_GESTURE_SWIPE_RIGHT
    )

    val HEADERS = mapOf(
        SWIPE_DOWN to R.string.header_fingerprint_gesture_down,
        SWIPE_UP to R.string.header_fingerprint_gesture_up,
        SWIPE_LEFT to R.string.header_fingerprint_gesture_left,
        SWIPE_RIGHT to R.string.header_fingerprint_gesture_right
    )
}

fun FingerprintMap.getFlagLabelList(ctx: Context): List<String> = sequence {
    FingerprintMap.FLAG_LABEL_MAP.keys.forEach { flag ->
        if (flags.hasFlag(flag)) {
            yield(ctx.str(FingerprintMap.FLAG_LABEL_MAP.getValue(flag)))
        }
    }
}.toList()

fun FingerprintMap.buildOptionsDescription(ctx: Context): String = buildString {
    getFlagLabelList(ctx).forEachIndexed { index, label ->
        if (index > 0) {
            append(" ${ctx.str(R.string.interpunct)} ")
        }

        append(label)
    }
}