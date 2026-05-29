package io.github.sds100.keymapper.base.actions.talkback

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.os.Handler
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityGestureUtils
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success

object TalkbackGesturePerformer {
    fun performTalkBackGesture(
        service: AccessibilityService,
        gesture: TalkBackGestureType,
        gestureHandler: Handler?,
    ): KMResult<*> {
        val dm = service.resources.displayMetrics
        val cx = dm.widthPixels / 2f
        val cy = dm.heightPixels / 2f
        // Use 40% of the smaller screen dimension as swipe length
        val swipeLen = minOf(dm.widthPixels, dm.heightPixels) * 0.4f
        // Finger spacing for multi-finger gestures (pixels)
        val fingerSpacing = dm.density * 40f

        val gestureBuilder = GestureDescription.Builder()

        when (gesture) {
            TalkBackGestureType.SWIPE_UP ->
                gestureBuilder.addStroke(
                    AccessibilityGestureUtils.buildSwipe(
                        cx,
                        cy,
                        cx,
                        cy - swipeLen,
                        200,
                    ),
                )

            TalkBackGestureType.SWIPE_DOWN ->
                gestureBuilder.addStroke(
                    AccessibilityGestureUtils.buildSwipe(
                        cx,
                        cy,
                        cx,
                        cy + swipeLen,
                        200,
                    ),
                )

            TalkBackGestureType.SWIPE_LEFT ->
                gestureBuilder.addStroke(
                    AccessibilityGestureUtils.buildSwipe(
                        cx,
                        cy,
                        cx - swipeLen,
                        cy,
                        200,
                    ),
                )

            TalkBackGestureType.SWIPE_RIGHT ->
                gestureBuilder.addStroke(
                    AccessibilityGestureUtils.buildSwipe(
                        cx,
                        cy,
                        cx + swipeLen,
                        cy,
                        200,
                    ),
                )

            TalkBackGestureType.SWIPE_UP_THEN_DOWN -> {
                val s1 = AccessibilityGestureUtils.buildChainedSwipe(
                    cx,
                    cy,
                    cx,
                    cy - swipeLen,
                    150,
                    willContinue = true,
                )
                val s2 = s1.continueStroke(
                    AccessibilityGestureUtils.buildPath(
                        cx,
                        cy - swipeLen,
                        cx,
                        cy,
                    ),
                    150,
                    150,
                    false,
                )
                gestureBuilder.addStroke(s1).addStroke(s2)
            }

            TalkBackGestureType.SWIPE_DOWN_THEN_UP -> {
                val s1 = AccessibilityGestureUtils.buildChainedSwipe(
                    cx,
                    cy,
                    cx,
                    cy + swipeLen,
                    150,
                    willContinue = true,
                )
                val s2 = s1.continueStroke(
                    AccessibilityGestureUtils.buildPath(
                        cx,
                        cy + swipeLen,
                        cx,
                        cy,
                    ),
                    150,
                    150,
                    false,
                )
                gestureBuilder.addStroke(s1).addStroke(s2)
            }

            TalkBackGestureType.SWIPE_LEFT_THEN_RIGHT -> {
                val s1 = AccessibilityGestureUtils.buildChainedSwipe(
                    cx,
                    cy,
                    cx - swipeLen,
                    cy,
                    150,
                    willContinue = true,
                )
                val s2 = s1.continueStroke(
                    AccessibilityGestureUtils.buildPath(
                        cx - swipeLen,
                        cy,
                        cx,
                        cy,
                    ),
                    150,
                    150,
                    false,
                )
                gestureBuilder.addStroke(s1).addStroke(s2)
            }

            TalkBackGestureType.SWIPE_RIGHT_THEN_LEFT -> {
                val s1 = AccessibilityGestureUtils.buildChainedSwipe(
                    cx,
                    cy,
                    cx + swipeLen,
                    cy,
                    150,
                    willContinue = true,
                )
                val s2 = s1.continueStroke(
                    AccessibilityGestureUtils.buildPath(
                        cx + swipeLen,
                        cy,
                        cx,
                        cy,
                    ),
                    150,
                    150,
                    false,
                )
                gestureBuilder.addStroke(s1).addStroke(s2)
            }

            TalkBackGestureType.SWIPE_RIGHT_THEN_UP -> {
                val s1 = AccessibilityGestureUtils.buildChainedSwipe(
                    cx,
                    cy,
                    cx + swipeLen,
                    cy,
                    150,
                    willContinue = true,
                )
                val s2 = s1.continueStroke(
                    AccessibilityGestureUtils.buildPath(cx + swipeLen, cy, cx, cy - swipeLen),
                    150,
                    150,
                    false,
                )
                gestureBuilder.addStroke(s1).addStroke(s2)
            }

            TalkBackGestureType.TWO_FINGER_TAP ->
                AccessibilityGestureUtils.addMultiFingerTaps(
                    gestureBuilder,
                    cx,
                    cy,
                    fingerSpacing,
                    fingerCount = 2,
                    tapCount = 1,
                    holdDuration = 50,
                )

            TalkBackGestureType.TWO_FINGER_DOUBLE_TAP_HOLD ->
                AccessibilityGestureUtils.addMultiFingerDoubleTapHold(
                    gestureBuilder,
                    cx,
                    cy,
                    fingerSpacing,
                    fingerCount = 2,
                )

            TalkBackGestureType.TWO_FINGER_TRIPLE_TAP ->
                AccessibilityGestureUtils.addMultiFingerTaps(
                    gestureBuilder,
                    cx,
                    cy,
                    fingerSpacing,
                    fingerCount = 2,
                    tapCount = 3,
                    holdDuration = 50,
                )

            TalkBackGestureType.TWO_FINGER_TRIPLE_TAP_HOLD ->
                AccessibilityGestureUtils.addMultiFingerTripleTapHold(
                    gestureBuilder,
                    cx,
                    cy,
                    fingerSpacing,
                    fingerCount = 2,
                )

            TalkBackGestureType.THREE_FINGER_TAP ->
                AccessibilityGestureUtils.addMultiFingerTaps(
                    gestureBuilder,
                    cx,
                    cy,
                    fingerSpacing,
                    fingerCount = 3,
                    tapCount = 1,
                    holdDuration = 50,
                )

            TalkBackGestureType.THREE_FINGER_TAP_HOLD ->
                AccessibilityGestureUtils.addMultiFingerTaps(
                    gestureBuilder,
                    cx,
                    cy,
                    fingerSpacing,
                    fingerCount = 3,
                    tapCount = 1,
                    holdDuration = 600,
                )

            TalkBackGestureType.THREE_FINGER_TRIPLE_TAP_HOLD ->
                AccessibilityGestureUtils.addMultiFingerTripleTapHold(
                    gestureBuilder,
                    cx,
                    cy,
                    fingerSpacing,
                    fingerCount = 3,
                )

            TalkBackGestureType.THREE_FINGER_SWIPE_UP ->
                AccessibilityGestureUtils.addMultiFingerSwipe(
                    gestureBuilder,
                    cx,
                    cy,
                    cx,
                    cy - swipeLen,
                    fingerSpacing,
                    fingerCount = 3,
                )

            TalkBackGestureType.THREE_FINGER_SWIPE_DOWN ->
                AccessibilityGestureUtils.addMultiFingerSwipe(
                    gestureBuilder,
                    cx,
                    cy,
                    cx,
                    cy + swipeLen,
                    fingerSpacing,
                    fingerCount = 3,
                )

            TalkBackGestureType.FOUR_FINGER_TAP ->
                AccessibilityGestureUtils.addMultiFingerTaps(
                    gestureBuilder,
                    cx,
                    cy,
                    fingerSpacing,
                    fingerCount = 4,
                    tapCount = 1,
                    holdDuration = 50,
                )

            TalkBackGestureType.FOUR_FINGER_DOUBLE_TAP ->
                AccessibilityGestureUtils.addMultiFingerTaps(
                    gestureBuilder,
                    cx,
                    cy,
                    fingerSpacing,
                    fingerCount = 4,
                    tapCount = 2,
                    holdDuration = 50,
                )

            TalkBackGestureType.FOUR_FINGER_SWIPE_UP ->
                AccessibilityGestureUtils.addMultiFingerSwipe(
                    gestureBuilder,
                    cx,
                    cy,
                    cx,
                    cy - swipeLen,
                    fingerSpacing,
                    fingerCount = 4,
                )

            TalkBackGestureType.FOUR_FINGER_SWIPE_DOWN ->
                AccessibilityGestureUtils.addMultiFingerSwipe(
                    gestureBuilder,
                    cx,
                    cy,
                    cx,
                    cy + swipeLen,
                    fingerSpacing,
                    fingerCount = 4,
                )

            TalkBackGestureType.FOUR_FINGER_SWIPE_LEFT ->
                AccessibilityGestureUtils.addMultiFingerSwipe(
                    gestureBuilder,
                    cx,
                    cy,
                    cx - swipeLen,
                    cy,
                    fingerSpacing,
                    fingerCount = 4,
                )

            TalkBackGestureType.FOUR_FINGER_SWIPE_RIGHT ->
                AccessibilityGestureUtils.addMultiFingerSwipe(
                    gestureBuilder,
                    cx,
                    cy,
                    cx + swipeLen,
                    cy,
                    fingerSpacing,
                    fingerCount = 4,
                )
        }

        val success = service.dispatchGesture(gestureBuilder.build(), null, gestureHandler)

        return if (success) {
            Success(Unit)
        } else {
            KMError.FailedToDispatchGesture
        }
    }
}
