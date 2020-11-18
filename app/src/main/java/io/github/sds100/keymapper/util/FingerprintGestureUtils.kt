package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.DataStoreKeys

/**
 * Created by sds100 on 14/11/20.
 */
object FingerprintGestureUtils {
    const val SWIPE_DOWN = "swipe_down"
    const val SWIPE_UP = "swipe_up"

    const val CHOOSE_ACTION_SWIPE_DOWN_REQUEST_KEY = "request_choose_action_swipe_down"
    const val CHOOSE_ACTION_SWIPE_UP_REQUEST_KEY = "request_choose_action_swipe_up"

    val GESTURES = arrayOf(SWIPE_DOWN, SWIPE_UP)

    val PREF_KEYS = mapOf(
        SWIPE_DOWN to DataStoreKeys.FINGERPRINT_GESTURE_SWIPE_DOWN,
        SWIPE_UP to DataStoreKeys.FINGERPRINT_GESTURE_SWIPE_UP
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