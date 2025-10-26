package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

object WindowSizeClassExt {
    operator fun WindowWidthSizeClass.compareTo(other: WindowWidthSizeClass): Int =
        when {
            this == WindowWidthSizeClass.COMPACT && other == WindowWidthSizeClass.MEDIUM -> -1
            this == WindowWidthSizeClass.COMPACT && other == WindowWidthSizeClass.EXPANDED -> -1
            this == WindowWidthSizeClass.MEDIUM && other == WindowWidthSizeClass.COMPACT -> 1
            this == WindowWidthSizeClass.MEDIUM && other == WindowWidthSizeClass.EXPANDED -> -1
            this == WindowWidthSizeClass.EXPANDED && other == WindowWidthSizeClass.COMPACT -> 1
            this == WindowWidthSizeClass.EXPANDED && other == WindowWidthSizeClass.MEDIUM -> 1
            else -> 0
        }

    operator fun WindowHeightSizeClass.compareTo(other: WindowHeightSizeClass): Int =
        when {
            this == WindowHeightSizeClass.COMPACT && other == WindowHeightSizeClass.MEDIUM -> -1
            this == WindowHeightSizeClass.COMPACT && other == WindowHeightSizeClass.EXPANDED -> -1
            this == WindowHeightSizeClass.MEDIUM && other == WindowHeightSizeClass.COMPACT -> 1
            this == WindowHeightSizeClass.MEDIUM && other == WindowHeightSizeClass.EXPANDED -> -1
            this == WindowHeightSizeClass.EXPANDED && other == WindowHeightSizeClass.COMPACT -> 1
            this == WindowHeightSizeClass.EXPANDED && other == WindowHeightSizeClass.MEDIUM -> 1
            else -> 0
        }
}
