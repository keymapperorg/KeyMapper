package io.github.sds100.keymapper.base.actions.talkback

enum class TalkBackGestureType {
    // 1-finger swipes
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,

    // 1-finger angular swipes (two-direction)
    SWIPE_UP_THEN_DOWN,
    SWIPE_DOWN_THEN_UP,
    SWIPE_LEFT_THEN_RIGHT,
    SWIPE_RIGHT_THEN_LEFT,
    SWIPE_RIGHT_THEN_UP,

    // 2-finger gestures
    TWO_FINGER_TAP,
    TWO_FINGER_DOUBLE_TAP_HOLD,
    TWO_FINGER_TRIPLE_TAP,
    TWO_FINGER_TRIPLE_TAP_HOLD,

    // 3-finger gestures
    THREE_FINGER_TAP,
    THREE_FINGER_TAP_HOLD,
    THREE_FINGER_TRIPLE_TAP_HOLD,
    THREE_FINGER_SWIPE_UP,
    THREE_FINGER_SWIPE_DOWN,

    // 4-finger gestures
    FOUR_FINGER_TAP,
    FOUR_FINGER_DOUBLE_TAP,
    FOUR_FINGER_SWIPE_UP,
    FOUR_FINGER_SWIPE_DOWN,
    FOUR_FINGER_SWIPE_LEFT,
    FOUR_FINGER_SWIPE_RIGHT,

    // NOTE: 4-finger triple tap is not possible.
    // Android limits GestureDescription to 10 strokes.
    // Four-finger triple-tap would require 12 strokes and is not included in the gesture set
    // for this reason.
}
