package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 14/11/20.
 */
object FingerprintGestureUtils {
    const val SWIPE_DOWN = "swipe_down"
    const val SWIPE_UP = "swipe_up"

    const val CHOOSE_ACTION_SWIPE_DOWN_REQUEST_KEY = "request_choose_action_swipe_down"
    const val CHOOSE_ACTION_SWIPE_UP_REQUEST_KEY = "request_choose_action_swipe_down"

    val GESTURES = arrayOf(SWIPE_DOWN, SWIPE_UP)

    val PREF_KEYS = mapOf(
        SWIPE_DOWN to R.string.key_pref_fingerprint_swipe_down_json,
        SWIPE_UP to R.string.key_pref_fingerprint_swipe_up_json
    )

    val HEADERS = mapOf(
        SWIPE_DOWN to R.string.header_fingerprint_gesture_down,
        SWIPE_UP to R.string.header_fingerprint_gesture_up
    )

    val CHOOSE_ACTION_REQUEST_KEYS = mapOf(
        SWIPE_DOWN to CHOOSE_ACTION_SWIPE_DOWN_REQUEST_KEY,
        SWIPE_UP to CHOOSE_ACTION_SWIPE_UP_REQUEST_KEY
    )
}