package io.github.sds100.keymapper.common.util

import android.content.Context
import android.graphics.Point
import android.util.Size
import android.util.TypedValue
import androidx.core.content.ContextCompat

fun dpToPx(context: Context, dp: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics,
    )
}

fun Context.getDisplayDensity(): Float {
    return resources.displayMetrics.density
}

fun Context.getRealDisplaySize(): SizeKM {
    val point = Point().apply {
        ContextCompat.getDisplayOrDefault(this@getRealDisplaySize).getRealSize(this)
    }

    return SizeKM(point.x, point.y)
}

fun Context.getApplicationDisplaySize(): Size {
    val point = Point().apply {
        ContextCompat.getDisplayOrDefault(this@getApplicationDisplaySize).getSize(this)
    }

    return Size(point.x, point.y)
}
