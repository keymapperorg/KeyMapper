package io.github.sds100.keymapper.util

import android.content.Context
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.DataStoreKeys
import io.github.sds100.keymapper.data.model.FingerprintGestureMap
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 14/11/20.
 */
object FingerprintGestureUtils {
    const val SWIPE_DOWN = "swipe_down"
    const val SWIPE_UP = "swipe_up"

    private const val CHOOSE_ACTION_SWIPE_DOWN_REQUEST_KEY = "request_choose_action_swipe_down"
    private const val CHOOSE_ACTION_SWIPE_UP_REQUEST_KEY = "request_choose_action_swipe_up"

    private const val OPTIONS_SWIPE_DOWN_REQUEST_KEY = "request_options_swipe_down"
    private const val OPTIONS_SWIPE_UP_REQUEST_KEY = "request_options_swipe_up"

    private const val ADD_CONSTRAINT_SWIPE_DOWN_REQUEST_KEY = "request_add_constraint_swipe_down"
    private const val ADD_CONSTRAINT_SWIPE_UP_REQUEST_KEY = "request_add_constraint_swipe_up"

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

    val OPTIONS_REQUEST_KEYS = mapOf(
        SWIPE_DOWN to OPTIONS_SWIPE_DOWN_REQUEST_KEY,
        SWIPE_UP to OPTIONS_SWIPE_UP_REQUEST_KEY
    )

    val ADD_CONSTRAINT_REQUEST_KEYS = mapOf(
        SWIPE_DOWN to ADD_CONSTRAINT_SWIPE_DOWN_REQUEST_KEY,
        SWIPE_UP to ADD_CONSTRAINT_SWIPE_UP_REQUEST_KEY
    )
}

fun FingerprintGestureMap.getFlagLabelList(ctx: Context): List<String> = sequence {
    FingerprintGestureMap.FLAG_LABEL_MAP.keys.forEach { flag ->
        if (flags.hasFlag(flag)) {
            yield(ctx.str(FingerprintGestureMap.FLAG_LABEL_MAP.getValue(flag)))
        }
    }
}.toList()

fun FingerprintGestureMap.buildOptionsDescription(ctx: Context): String = buildString {
    getFlagLabelList(ctx).forEachIndexed { index, label ->
        if (index > 0) {
            append(" ${ctx.str(R.string.interpunct)} ")
        }

        append(label)
    }
}