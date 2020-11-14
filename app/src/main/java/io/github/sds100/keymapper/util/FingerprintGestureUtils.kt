package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 14/11/20.
 */
object FingerprintGestureUtils {
    const val SWIPE_DOWN = "swipe_down"
    const val SWIPE_UP = "swipe_up"

    val GESTURES = arrayOf(SWIPE_DOWN, SWIPE_UP)

    val PREF_KEYS = mapOf(
        SWIPE_DOWN to R.string.key_pref_fingerprint_swipe_down_json,
        SWIPE_UP to R.string.key_pref_fingerprint_swipe_up_json
    )

    val HEADERS = mapOf(
        SWIPE_DOWN to R.string.header_fingerprint_gesture_down,
        SWIPE_UP to R.string.header_fingerprint_gesture_up
    )
}