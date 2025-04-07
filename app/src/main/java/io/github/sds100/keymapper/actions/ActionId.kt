package io.github.sds100.keymapper.actions

/**
 * Created by sds100 on 15/03/2021.
 */
enum class ActionId {
    APP,
    APP_SHORTCUT,
    KEY_CODE,
    KEY_EVENT,
    TAP_SCREEN,
    SWIPE_SCREEN,
    PINCH_SCREEN,
    TEXT,
    URL,
    INTENT,
    PHONE_CALL,
    SOUND,

    TOGGLE_WIFI,
    ENABLE_WIFI,
    DISABLE_WIFI,

    TOGGLE_BLUETOOTH,
    ENABLE_BLUETOOTH,
    DISABLE_BLUETOOTH,

    TOGGLE_MOBILE_DATA,
    ENABLE_MOBILE_DATA,
    DISABLE_MOBILE_DATA,

    TOGGLE_AUTO_BRIGHTNESS,
    DISABLE_AUTO_BRIGHTNESS,
    ENABLE_AUTO_BRIGHTNESS,
    INCREASE_BRIGHTNESS,
    DECREASE_BRIGHTNESS,

    TOGGLE_AUTO_ROTATE,
    ENABLE_AUTO_ROTATE,
    DISABLE_AUTO_ROTATE,
    PORTRAIT_MODE,
    LANDSCAPE_MODE,
    SWITCH_ORIENTATION,
    CYCLE_ROTATIONS,

    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_SHOW_DIALOG,
    VOLUME_DECREASE_STREAM,
    VOLUME_INCREASE_STREAM,
    CYCLE_RINGER_MODE,
    CHANGE_RINGER_MODE,
    CYCLE_VIBRATE_RING,
    TOGGLE_DND_MODE,
    ENABLE_DND_MODE,
    DISABLE_DND_MODE,
    VOLUME_UNMUTE,
    VOLUME_MUTE,
    VOLUME_TOGGLE_MUTE,

    EXPAND_NOTIFICATION_DRAWER,
    TOGGLE_NOTIFICATION_DRAWER,
    EXPAND_QUICK_SETTINGS,
    TOGGLE_QUICK_SETTINGS,
    COLLAPSE_STATUS_BAR,

    PAUSE_MEDIA,
    PAUSE_MEDIA_PACKAGE,
    PLAY_MEDIA,
    PLAY_MEDIA_PACKAGE,
    PLAY_PAUSE_MEDIA,
    PLAY_PAUSE_MEDIA_PACKAGE,
    NEXT_TRACK,
    NEXT_TRACK_PACKAGE,
    PREVIOUS_TRACK,
    PREVIOUS_TRACK_PACKAGE,
    FAST_FORWARD,
    FAST_FORWARD_PACKAGE,
    REWIND,
    REWIND_PACKAGE,

    GO_BACK,
    GO_HOME,
    OPEN_RECENTS,
    TOGGLE_SPLIT_SCREEN,
    GO_LAST_APP,
    OPEN_MENU,

    TOGGLE_FLASHLIGHT,
    ENABLE_FLASHLIGHT,
    DISABLE_FLASHLIGHT,
    CHANGE_FLASHLIGHT_STRENGTH,

    ENABLE_NFC,
    DISABLE_NFC,
    TOGGLE_NFC,

    MOVE_CURSOR_TO_END,
    TOGGLE_KEYBOARD,
    SHOW_KEYBOARD,
    HIDE_KEYBOARD,
    SHOW_KEYBOARD_PICKER,
    TEXT_CUT,
    TEXT_COPY,
    TEXT_PASTE,
    SELECT_WORD_AT_CURSOR,

    SWITCH_KEYBOARD,

    TOGGLE_AIRPLANE_MODE,
    ENABLE_AIRPLANE_MODE,
    DISABLE_AIRPLANE_MODE,

    SCREENSHOT,

    OPEN_VOICE_ASSISTANT,
    OPEN_DEVICE_ASSISTANT,
    OPEN_CAMERA,
    LOCK_DEVICE,
    POWER_ON_OFF_DEVICE,
    SECURE_LOCK_DEVICE,
    CONSUME_KEY_EVENT,
    OPEN_SETTINGS,
    SHOW_POWER_MENU,

    DISMISS_MOST_RECENT_NOTIFICATION,
    DISMISS_ALL_NOTIFICATIONS,

    ANSWER_PHONE_CALL,
    END_PHONE_CALL,
    DEVICE_CONTROLS,

    HTTP_REQUEST,
}
