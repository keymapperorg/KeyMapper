package io.github.sds100.keymapper.base.util.ui.compose

import androidx.compose.ui.graphics.vector.ImageVector

sealed class ComposeIconInfo {
    data class Vector(val imageVector: ImageVector) : ComposeIconInfo()
    data class Drawable(val drawable: android.graphics.drawable.Drawable) : ComposeIconInfo()
}
