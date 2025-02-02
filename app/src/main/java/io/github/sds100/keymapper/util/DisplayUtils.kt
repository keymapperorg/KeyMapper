package io.github.sds100.keymapper.util

import android.content.Context
import android.util.TypedValue

fun dpToPx(context: Context, dp: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics,
    )
}
