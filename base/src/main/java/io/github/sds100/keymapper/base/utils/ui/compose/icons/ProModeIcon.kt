package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.ProModeIcon: ImageVector
    get() {
        if (_ProMode != null) {
            return _ProMode!!
        }
        _ProMode =
            ImageVector
                .Builder(
                    name = "ProMode",
                    defaultWidth = 32.dp,
                    defaultHeight = 32.dp,
                    viewportWidth = 32f,
                    viewportHeight = 32f,
                ).apply {
                    group(
                        clipPathData =
                            PathData {
                                moveTo(0f, 0f)
                                lineTo(32f, 0f)
                                lineTo(32f, 32f)
                                lineTo(0f, 32f)
                                close()
                            },
                    ) {
                    }
                    group(
                        clipPathData =
                            PathData {
                                moveTo(0f, 0f)
                                lineTo(32f, 0f)
                                lineTo(32f, 32f)
                                lineTo(0f, 32f)
                                close()
                            },
                    ) {
                    }
                    group(
                        clipPathData =
                            PathData {
                                moveTo(-0f, -0f)
                                lineTo(32f, -0f)
                                lineTo(32f, 32f)
                                lineTo(-0f, 32f)
                                close()
                            },
                    ) {
                    }
                    group(
                        clipPathData =
                            PathData {
                                moveTo(-0f, 32f)
                                lineTo(32f, 32f)
                                lineTo(32f, -0f)
                                lineTo(-0f, -0f)
                                close()
                            },
                    ) {
                    }
                    group(
                        clipPathData =
                            PathData {
                                moveTo(0f, 0f)
                                lineTo(32f, 0f)
                                lineTo(32f, 32f)
                                lineTo(0f, 32f)
                                close()
                            },
                    ) {
                    }
                    group(
                        clipPathData =
                            PathData {
                                moveTo(-0f, 32f)
                                lineTo(32f, 32f)
                                lineTo(32f, -0f)
                                lineTo(-0f, -0f)
                                close()
                            },
                    ) {
                    }
                    group(
                        clipPathData =
                            PathData {
                                moveTo(-0f, 32f)
                                lineTo(32f, 32f)
                                lineTo(32f, -0f)
                                lineTo(-0f, -0f)
                                close()
                            },
                    ) {
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveToRelative(4f, 11f)
                        verticalLineToRelative(10f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(-4f)
                        horizontalLineToRelative(2f)
                        arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, -2f)
                        verticalLineTo(13f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 8f, 11f)
                        horizontalLineTo(4f)
                        moveToRelative(2f, 2f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(6f)
                        close()
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveToRelative(13f, 11f)
                        verticalLineToRelative(10f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(-4f)
                        horizontalLineToRelative(0.8f)
                        lineToRelative(1.2f, 4f)
                        horizontalLineToRelative(2f)
                        lineTo(17.76f, 16.85f)
                        curveTo(18.5f, 16.55f, 19f, 15.84f, 19f, 15f)
                        verticalLineToRelative(-2f)
                        arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, -2f)
                        horizontalLineToRelative(-4f)
                        moveToRelative(2f, 2f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(2f)
                        horizontalLineToRelative(-2f)
                        close()
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveToRelative(24f, 11f)
                        arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, 2f)
                        verticalLineToRelative(6f)
                        arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, 2f)
                        horizontalLineToRelative(2f)
                        arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, -2f)
                        verticalLineToRelative(-6f)
                        arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, -2f)
                        horizontalLineToRelative(-2f)
                        moveToRelative(0f, 2f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(6f)
                        horizontalLineToRelative(-2f)
                        close()
                    }
                }.build()

        return _ProMode!!
    }

@Suppress("ObjectPropertyName")
private var _ProMode: ImageVector? = null
