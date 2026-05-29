package io.github.sds100.keymapper.base.system.accessibility

import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path

object AccessibilityGestureUtils {

    fun buildSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): StrokeDescription =
        StrokeDescription(buildPath(x1, y1, x2, y2), 0, duration)

    fun buildChainedSwipe(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        duration: Long,
        willContinue: Boolean,
    ): StrokeDescription = StrokeDescription(buildPath(x1, y1, x2, y2), 0, duration, willContinue)

    fun fingerPositions(
        cx: Float,
        cy: Float,
        spacing: Float,
        count: Int,
    ): List<Pair<Float, Float>> {
        val total = (count - 1) * spacing
        val start = cx - total / 2f
        return (0 until count).map { i -> Pair(start + i * spacing, cy) }
    }

    fun addMultiFingerTaps(
        builder: GestureDescription.Builder,
        cx: Float,
        cy: Float,
        spacing: Float,
        fingerCount: Int,
        tapCount: Int,
        holdDuration: Long,
    ) {
        val positions = fingerPositions(cx, cy, spacing, fingerCount)
        val tapInterval = 200L
        for (tap in 0 until tapCount) {
            val startTime = tap * tapInterval
            for ((x, y) in positions) {
                val path = Path().apply { moveTo(x, y) }
                builder.addStroke(StrokeDescription(path, startTime, holdDuration))
            }
        }
    }

    fun addMultiFingerDoubleTapHold(
        builder: GestureDescription.Builder,
        cx: Float,
        cy: Float,
        spacing: Float,
        fingerCount: Int,
    ) {
        val positions = fingerPositions(cx, cy, spacing, fingerCount)
        for ((x, y) in positions) {
            val path = Path().apply { moveTo(x, y) }
            builder.addStroke(StrokeDescription(path, 0, 50))
            builder.addStroke(StrokeDescription(path, 200, 600))
        }
    }

    fun addMultiFingerTripleTapHold(
        builder: GestureDescription.Builder,
        cx: Float,
        cy: Float,
        spacing: Float,
        fingerCount: Int,
    ) {
        val positions = fingerPositions(cx, cy, spacing, fingerCount)
        for ((x, y) in positions) {
            val path = Path().apply { moveTo(x, y) }
            builder.addStroke(StrokeDescription(path, 0, 50))
            builder.addStroke(StrokeDescription(path, 200, 50))
            builder.addStroke(StrokeDescription(path, 400, 600))
        }
    }

    fun addMultiFingerSwipe(
        builder: GestureDescription.Builder,
        xStart: Float,
        yStart: Float,
        xEnd: Float,
        yEnd: Float,
        spacing: Float,
        fingerCount: Int,
    ) {
        val startPositions = fingerPositions(xStart, yStart, spacing, fingerCount)
        val endPositions = fingerPositions(xEnd, yEnd, spacing, fingerCount)
        for (i in 0 until fingerCount) {
            val (sx, sy) = startPositions[i]
            val (ex, ey) = endPositions[i]
            builder.addStroke(StrokeDescription(buildPath(sx, sy, ex, ey), 0, 200))
        }
    }

    fun buildPath(x1: Float, y1: Float, x2: Float, y2: Float): Path = Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y2)
    }
}
