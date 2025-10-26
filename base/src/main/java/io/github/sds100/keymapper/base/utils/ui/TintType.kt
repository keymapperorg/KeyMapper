package io.github.sds100.keymapper.base.utils.ui

import androidx.annotation.ColorInt

sealed class TintType {
    object None : TintType()

    object OnSurface : TintType()

    object Error : TintType()

    data class Color(
        @ColorInt
        val color: Int,
    ) : TintType()
}
