package io.github.sds100.keymapper.base.system.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.SHOW_MODE_AUTO
import android.accessibilityservice.AccessibilityService.SHOW_MODE_HIDDEN
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.annotation.SuppressLint
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.os.bundleOf
import io.github.sds100.keymapper.common.utils.InputEventType
import io.github.sds100.keymapper.common.utils.MathUtils
import io.github.sds100.keymapper.common.utils.PinchScreenType
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.Success

fun AccessibilityService.performActionOnNode(
    findNode: (node: AccessibilityNodeModel) -> Boolean,
    performAction: (node: AccessibilityNodeModel) -> AccessibilityNodeAction?,
): Result<*> {
    val node = rootInActiveWindow.findNodeRecursively {
        findNode(it.toModel())
    }

    if (node == null) {
        return Error.FailedToFindAccessibilityNode
    }

    val (action, extras) = performAction(node.toModel()) ?: return Success(Unit)

    node.performAction(action, bundleOf(*extras.toList().toTypedArray()))
    node.recycle()

    return Success(Unit)
}

fun AccessibilityService.hideKeyboard() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        softKeyboardController.showMode = SHOW_MODE_HIDDEN
    }
}

fun AccessibilityService.showKeyboard() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        softKeyboardController.showMode = SHOW_MODE_AUTO
    }
}

fun AccessibilityService.switchIme(imeId: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        softKeyboardController.switchToInputMethod(imeId)
    }
}

fun AccessibilityService.doGlobalAction(action: Int): Result<*> {
    val success = performGlobalAction(action)

    if (success) {
        return Success(Unit)
    } else {
        return Error.FailedToPerformAccessibilityGlobalAction(action)
    }
}

fun AccessibilityService.findFocussedNode(focus: Int): AccessibilityNodeModel? = findFocus(focus)?.toModel()

fun AccessibilityService.setInputMethodEnabled(imeId: String, enabled: Boolean) {
    @SuppressLint("CheckResult")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        softKeyboardController.setInputMethodEnabled(imeId, enabled)
    }
}

fun AccessibilityService.pinchScreen(
    x: Int,
    y: Int,
    distance: Int,
    pinchType: PinchScreenType,
    fingerCount: Int,
    duration: Int,
): Result<*> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        if (fingerCount >= GestureDescription.getMaxStrokeCount()) {
            return Error.GestureStrokeCountTooHigh
        }
        if (duration >= GestureDescription.getMaxGestureDuration()) {
            return Error.GestureDurationTooHigh
        }

        val gestureBuilder = GestureDescription.Builder()
        val distributedPoints: List<Point> =
            MathUtils.distributePointsOnCircle(Point(x, y), distance.toFloat() / 2, fingerCount)

        for (index in distributedPoints.indices) {
            val p = Path()
            if (pinchType == PinchScreenType.PINCH_IN) {
                p.moveTo(x.toFloat(), y.toFloat())
                p.lineTo(
                    distributedPoints[index].x.toFloat(),
                    distributedPoints[index].y.toFloat(),
                )
            } else {
                p.moveTo(
                    distributedPoints[index].x.toFloat(),
                    distributedPoints[index].y.toFloat(),
                )
                p.lineTo(x.toFloat(), y.toFloat())
            }

            gestureBuilder.addStroke(StrokeDescription(p, 0, duration.toLong()))
        }

        val success = dispatchGesture(gestureBuilder.build(), null, null)

        return if (success) {
            Success(Unit)
        } else {
            Error.FailedToDispatchGesture
        }
    }

    return Error.SdkVersionTooLow(Build.VERSION_CODES.N)
}

fun AccessibilityService.tapScreen(x: Int, y: Int, inputEventType: InputEventType): Result<*> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val duration = 1L // ms

        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }

        val strokeDescription =
            when {
                inputEventType == InputEventType.DOWN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    StrokeDescription(
                        path,
                        0,
                        duration,
                        true,
                    )

                inputEventType == InputEventType.UP && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    StrokeDescription(
                        path,
                        59999,
                        duration,
                        false,
                    )

                else -> StrokeDescription(path, 0, duration)
            }

        strokeDescription.let {
            val gestureDescription = GestureDescription.Builder().apply {
                addStroke(it)
            }.build()

            val success = dispatchGesture(gestureDescription, null, null)

            return if (success) {
                Success(Unit)
            } else {
                Error.FailedToDispatchGesture
            }
        }
    }

    return Error.SdkVersionTooLow(Build.VERSION_CODES.N)
}

fun AccessibilityService.swipeScreen(
    xStart: Int,
    yStart: Int,
    xEnd: Int,
    yEnd: Int,
    fingerCount: Int,
    duration: Int,
): Result<*> {
    // virtual distance between fingers on multitouch gestures
    val fingerGestureDistance = 10L

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        if (fingerCount >= GestureDescription.getMaxStrokeCount()) {
            return Error.GestureStrokeCountTooHigh
        }
        if (duration >= GestureDescription.getMaxGestureDuration()) {
            return Error.GestureDurationTooHigh
        }

        val pStart = Point(xStart, yStart)
        val pEnd = Point(xEnd, yEnd)

        val gestureBuilder = GestureDescription.Builder()

        if (fingerCount == 1) {
            val p = Path()
            p.moveTo(pStart.x.toFloat(), pStart.y.toFloat())
            p.lineTo(pEnd.x.toFloat(), pEnd.y.toFloat())
            gestureBuilder.addStroke(StrokeDescription(p, 0, duration.toLong()))
        } else {
            // segments between fingers
            val segmentCount = fingerCount - 1
            // the line of the perpendicular line which will be created to place the virtual fingers on it
            val perpendicularLineLength = (fingerGestureDistance * fingerCount).toInt()

            // the length of each segment between fingers
            val segmentLength = perpendicularLineLength / segmentCount
            // perpendicular line of the start swipe point
            val perpendicularLineStart = MathUtils.getPerpendicularOfLine(
                pStart,
                pEnd,
                perpendicularLineLength,
            )
            // perpendicular line of the end swipe point
            val perpendicularLineEnd = MathUtils.getPerpendicularOfLine(
                pEnd,
                pStart,
                perpendicularLineLength,
                true,
            )

            // this is the angle between start and end point to rotate all virtual fingers on the perpendicular lines in the same direction
            val angle =
                MathUtils.angleBetweenPoints(Point(xStart, yStart), Point(xEnd, yEnd)) - 90

            // create the virtual fingers
            for (index in 0..segmentCount) {
                // offset of each finger
                val fingerOffsetLength = index * segmentLength * 2
                // move the coordinates of the current virtual finger on the perpendicular line for the start coordinates
                val startFingerCoordinateWithOffset =
                    MathUtils.movePointByDistanceAndAngle(
                        perpendicularLineStart.start,
                        fingerOffsetLength,
                        angle,
                    )
                // move the coordinates of the current virtual finger on the perpendicular line for the end coordinates
                val endFingerCoordinateWithOffset =
                    MathUtils.movePointByDistanceAndAngle(
                        perpendicularLineEnd.start,
                        fingerOffsetLength,
                        angle,
                    )

                // create a path for each finger, move the the coordinates on the perpendicular line and draw it to the end coordinates of the perpendicular line of the end swipe point
                val p = Path()
                p.moveTo(
                    startFingerCoordinateWithOffset.x.toFloat(),
                    startFingerCoordinateWithOffset.y.toFloat(),
                )
                p.lineTo(
                    endFingerCoordinateWithOffset.x.toFloat(),
                    endFingerCoordinateWithOffset.y.toFloat(),
                )

                gestureBuilder.addStroke(StrokeDescription(p, 0, duration.toLong()))
            }
        }

        val success = dispatchGesture(gestureBuilder.build(), null, null)

        return if (success) {
            Success(Unit)
        } else {
            Error.FailedToDispatchGesture
        }
    }

    return Error.SdkVersionTooLow(Build.VERSION_CODES.N)
}

/**
 * @return The node to find. Returns null if the node doesn't match the predicate
 */
fun AccessibilityNodeInfo?.findNodeRecursively(
    nodeInfo: AccessibilityNodeInfo? = this,
    depth: Int = 0,
    predicate: (node: AccessibilityNodeInfo) -> Boolean,
): AccessibilityNodeInfo? {
    if (nodeInfo == null) return null

    if (predicate(nodeInfo)) return nodeInfo

    for (i in 0 until nodeInfo.childCount) {
        val node = findNodeRecursively(nodeInfo.getChild(i), depth + 1, predicate)

        if (node != null) {
            return node
        }
    }

    return null
}

fun AccessibilityNodeInfo.toModel(): AccessibilityNodeModel = AccessibilityNodeModel(
    packageName = packageName?.toString(),
    contentDescription = contentDescription?.toString(),
    isFocused = isFocused,
    textSelectionStart = textSelectionStart,
    textSelectionEnd = textSelectionEnd,
    text = text?.toString(),
    isEditable = isEditable,
    className = className?.toString(),
    uniqueId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        uniqueId
    } else {
        null
    },
    viewResourceId = viewIdResourceName,
    actions = actionList.map { it.id },
)
