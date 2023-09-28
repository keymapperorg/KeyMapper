package io.github.sds100.keymapper.util

import android.graphics.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class Line(
    val start: Point,
    val end: Point
)

object MathUtils {
    fun deg2rad(degrees: Double): Double {
        return degrees * Math.PI / 180
    }

    fun rad2deg(radians: Double): Double {
        return radians * 180 / Math.PI
    }

    fun getPerpendicularOfLine(
        p1: Point,
        p2: Point,
        length: Int,
        reverse: Boolean = false
    ): Line {
        var px = p1.y - p2.y
        var py = p2.x - p1.x

        val len = (length / hypot(px.toFloat(), py.toFloat())).toInt()
        px *= len
        py *= len

        val start = Point(p1.x + px, p1.y + py)
        val end = Point(p1.x - px, p1.y - py)

        return if (!reverse) {
            Line(start, end)
        } else {
            Line(end, start)
        }
    }

    fun movePointByDistanceAndAngle(
        p: Point,
        distance: Int,
        degrees: Double,
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int
    ): Point {
        val newX =
            (p.x + cos(deg2rad(degrees)) * distance).toInt().coerceAtLeast(minX).coerceAtMost(maxX)
        val newY =
            (p.y + sin(deg2rad(degrees)) * distance).toInt().coerceAtLeast(minY).coerceAtMost(maxY)

        return Point(newX, newY)
    }

    fun angleBetweenPoints(p1: Point, p2: Point): Double {
        return rad2deg(atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble()))
    }

    fun distributePointsOnCircle(
        circleCenter: Point,
        circleRadius: Float,
        numPoints: Int,
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int
    ): List<Point> {

        val points = arrayListOf<Point>()
        var angle: Double = 0.0
        val step = (2 * Math.PI) / numPoints

        for (index in 0 until numPoints) {
            points.add(
                Point(
                    (circleCenter.x + circleRadius * cos(angle)).toInt().coerceAtLeast(minX)
                        .coerceAtMost(maxX),
                    (circleCenter.y + circleRadius * sin(angle)).toInt().coerceAtLeast(minY)
                        .coerceAtMost(maxY)
                )
            )
            angle += step
        }

        return points

    }

}