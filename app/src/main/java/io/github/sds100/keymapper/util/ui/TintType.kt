package io.github.sds100.keymapper.util.ui

import androidx.annotation.ColorInt

/**
 * Created by sds100 on 03/04/2020.
 */
sealed class TintType {
    object None : TintType()
    object OnSurface : TintType()
    object Error : TintType()
    data class Color(
        @ColorInt
        val color: Int,
    ) : TintType()
}
