package io.github.sds100.keymapper.base.actions.talkback

import io.github.sds100.keymapper.base.R

object TalkBackGestureStrings {
    fun getActionLabel(gesture: TalkBackGestureType): Int = when (gesture) {
        TalkBackGestureType.SWIPE_UP ->
            R.string.talkback_gesture_action_swipe_up

        TalkBackGestureType.SWIPE_DOWN ->
            R.string.talkback_gesture_action_swipe_down

        TalkBackGestureType.SWIPE_LEFT ->
            R.string.talkback_gesture_action_swipe_left

        TalkBackGestureType.SWIPE_RIGHT ->
            R.string.talkback_gesture_action_swipe_right

        TalkBackGestureType.SWIPE_UP_THEN_DOWN ->
            R.string.talkback_gesture_action_swipe_up_then_down

        TalkBackGestureType.SWIPE_DOWN_THEN_UP ->
            R.string.talkback_gesture_action_swipe_down_then_up

        TalkBackGestureType.SWIPE_LEFT_THEN_RIGHT ->
            R.string.talkback_gesture_action_swipe_left_then_right

        TalkBackGestureType.SWIPE_RIGHT_THEN_LEFT ->
            R.string.talkback_gesture_action_swipe_right_then_left

        TalkBackGestureType.SWIPE_RIGHT_THEN_UP ->
            R.string.talkback_gesture_action_swipe_right_then_up

        TalkBackGestureType.TWO_FINGER_TAP ->
            R.string.talkback_gesture_action_two_finger_tap

        TalkBackGestureType.TWO_FINGER_DOUBLE_TAP_HOLD ->
            R.string.talkback_gesture_action_two_finger_double_tap_hold

        TalkBackGestureType.TWO_FINGER_TRIPLE_TAP ->
            R.string.talkback_gesture_action_two_finger_triple_tap

        TalkBackGestureType.TWO_FINGER_TRIPLE_TAP_HOLD ->
            R.string.talkback_gesture_action_two_finger_triple_tap_hold

        TalkBackGestureType.THREE_FINGER_TAP ->
            R.string.talkback_gesture_action_three_finger_tap

        TalkBackGestureType.THREE_FINGER_TAP_HOLD ->
            R.string.talkback_gesture_action_three_finger_tap_hold

        TalkBackGestureType.THREE_FINGER_TRIPLE_TAP_HOLD ->
            R.string.talkback_gesture_action_three_finger_triple_tap_hold

        TalkBackGestureType.THREE_FINGER_SWIPE_UP ->
            R.string.talkback_gesture_action_three_finger_swipe_up

        TalkBackGestureType.THREE_FINGER_SWIPE_DOWN ->
            R.string.talkback_gesture_action_three_finger_swipe_down

        TalkBackGestureType.FOUR_FINGER_TAP ->
            R.string.talkback_gesture_action_four_finger_tap

        TalkBackGestureType.FOUR_FINGER_DOUBLE_TAP ->
            R.string.talkback_gesture_action_four_finger_double_tap

        TalkBackGestureType.FOUR_FINGER_SWIPE_UP ->
            R.string.talkback_gesture_action_four_finger_swipe_up

        TalkBackGestureType.FOUR_FINGER_SWIPE_DOWN ->
            R.string.talkback_gesture_action_four_finger_swipe_down

        TalkBackGestureType.FOUR_FINGER_SWIPE_LEFT ->
            R.string.talkback_gesture_action_four_finger_swipe_left

        TalkBackGestureType.FOUR_FINGER_SWIPE_RIGHT ->
            R.string.talkback_gesture_action_four_finger_swipe_right
    }

    fun getGestureLabel(gesture: TalkBackGestureType): Int = when (gesture) {
        TalkBackGestureType.SWIPE_UP ->
            R.string.talkback_gesture_name_swipe_up

        TalkBackGestureType.SWIPE_DOWN ->
            R.string.talkback_gesture_name_swipe_down

        TalkBackGestureType.SWIPE_LEFT ->
            R.string.talkback_gesture_name_swipe_left

        TalkBackGestureType.SWIPE_RIGHT ->
            R.string.talkback_gesture_name_swipe_right

        TalkBackGestureType.SWIPE_UP_THEN_DOWN ->
            R.string.talkback_gesture_name_swipe_up_then_down

        TalkBackGestureType.SWIPE_DOWN_THEN_UP ->
            R.string.talkback_gesture_name_swipe_down_then_up

        TalkBackGestureType.SWIPE_LEFT_THEN_RIGHT ->
            R.string.talkback_gesture_name_swipe_left_then_right

        TalkBackGestureType.SWIPE_RIGHT_THEN_LEFT ->
            R.string.talkback_gesture_name_swipe_right_then_left

        TalkBackGestureType.SWIPE_RIGHT_THEN_UP ->
            R.string.talkback_gesture_name_swipe_right_then_up

        TalkBackGestureType.TWO_FINGER_TAP ->
            R.string.talkback_gesture_name_two_finger_tap

        TalkBackGestureType.TWO_FINGER_DOUBLE_TAP_HOLD ->
            R.string.talkback_gesture_name_two_finger_double_tap_hold

        TalkBackGestureType.TWO_FINGER_TRIPLE_TAP ->
            R.string.talkback_gesture_name_two_finger_triple_tap

        TalkBackGestureType.TWO_FINGER_TRIPLE_TAP_HOLD ->
            R.string.talkback_gesture_name_two_finger_triple_tap_hold

        TalkBackGestureType.THREE_FINGER_TAP ->
            R.string.talkback_gesture_name_three_finger_tap

        TalkBackGestureType.THREE_FINGER_TAP_HOLD ->
            R.string.talkback_gesture_name_three_finger_tap_hold

        TalkBackGestureType.THREE_FINGER_TRIPLE_TAP_HOLD ->
            R.string.talkback_gesture_name_three_finger_triple_tap_hold

        TalkBackGestureType.THREE_FINGER_SWIPE_UP ->
            R.string.talkback_gesture_name_three_finger_swipe_up

        TalkBackGestureType.THREE_FINGER_SWIPE_DOWN ->
            R.string.talkback_gesture_name_three_finger_swipe_down

        TalkBackGestureType.FOUR_FINGER_TAP ->
            R.string.talkback_gesture_name_four_finger_tap

        TalkBackGestureType.FOUR_FINGER_DOUBLE_TAP ->
            R.string.talkback_gesture_name_four_finger_double_tap

        TalkBackGestureType.FOUR_FINGER_SWIPE_UP ->
            R.string.talkback_gesture_name_four_finger_swipe_up

        TalkBackGestureType.FOUR_FINGER_SWIPE_DOWN ->
            R.string.talkback_gesture_name_four_finger_swipe_down

        TalkBackGestureType.FOUR_FINGER_SWIPE_LEFT ->
            R.string.talkback_gesture_name_four_finger_swipe_left

        TalkBackGestureType.FOUR_FINGER_SWIPE_RIGHT ->
            R.string.talkback_gesture_name_four_finger_swipe_right
    }
}
