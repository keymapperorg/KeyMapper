package io.github.sds100.keymapper

/**
 * Created by sds100 on 04/08/2018.
 */
object SystemAction {
    //wifi
    const val TOGGLE_WIFI = "toggle_wifi"
    const val ENABLE_WIFI = "enable_wifi"
    const val DISABLE_WIFI = "disable_wifi"

    //bluetooth
    const val TOGGLE_BLUETOOTH = "toggle_bluetooth"
    const val ENABLE_BLUETOOTH = "enable_bluetooth"
    const val DISABLE_BLUETOOTH = "disable_bluetooth"

    //mobile data
    /**
     * REQUIRES ROOT
     */
    const val TOGGLE_MOBILE_DATA = "toggle_mobile_data"

    /**
     * REQUIRES ROOT
     */
    const val ENABLE_MOBILE_DATA = "enable_mobile_data"

    /**
     * REQUIRES ROOT
     */
    const val DISABLE_MOBILE_DATA = "disable_mobile_data"

    //brightness
    const val TOGGLE_AUTO_BRIGHTNESS = "toggle_auto_brightness"
    const val DISABLE_AUTO_BRIGHTNESS = "disable_auto_brightness"
    const val ENABLE_AUTO_BRIGHTNESS = "enable_auto_brightness"
    const val INCREASE_BRIGHTNESS = "increase_brightness"
    const val DECREASE_BRIGHTNESS = "decrease_brightness"

    //auto-rotate
    const val TOGGLE_AUTO_ROTATE = "toggle_auto_rotate"
    const val ENABLE_AUTO_ROTATE = "enable_auto_rotate"
    const val DISABLE_AUTO_ROTATE = "disable_auto_rotate"
    const val PORTRAIT_MODE = "portrait_mode"
    const val LANDSCAPE_MODE = "landscape_mode"
    const val SWITCH_ORIENTATION = "switch_orientation"

    //volume
    const val VOLUME_UP = "volume_up"
    const val VOLUME_DOWN = "volume_down"
    const val VOLUME_SHOW_DIALOG = "volume_show_dialog"
    const val VOLUME_DECREASE_STREAM = "volume_decrease_stream"
    const val VOLUME_INCREASE_STREAM = "volume_increase_stream"
    const val CYCLE_RINGER_MODE = "ringer_mode_cycle"
    const val CHANGE_RINGER_MODE = "ringer_mode_change"

    const val VOLUME_UNMUTE = "volume_unmute"
    const val VOLUME_MUTE = "volume_mute"
    const val VOLUME_TOGGLE_MUTE = "volume_toggle_mute"

    //status bar
    const val EXPAND_NOTIFICATION_DRAWER = "expand_notification_drawer"
    const val EXPAND_QUICK_SETTINGS = "expand_quick_settings"
    const val COLLAPSE_STATUS_BAR = "collapse_status_bar"

    //media
    const val PAUSE_MEDIA = "pause_media"
    const val PLAY_MEDIA = "play_media"
    const val PLAY_PAUSE_MEDIA = "play_pause_media"
    const val NEXT_TRACK = "next_track"
    const val PREVIOUS_TRACK = "previous_track"
    const val FAST_FORWARD = "fast_forward"
    const val REWIND = "rewind"

    //navigation
    const val GO_BACK = "go_back"
    const val GO_HOME = "go_home"
    const val OPEN_RECENTS = "open_recents"

    //flashlight
    const val TOGGLE_FLASHLIGHT = "toggle_flashlight"
    const val ENABLE_FLASHLIGHT = "enable_flashlight"
    const val DISABLE_FLASHLIGHT = "disable_flashlight"

    //NFC
    const val ENABLE_NFC = "nfc_enable"
    const val DISABLE_NFC = "nfc_disable"
    const val TOGGLE_NFC = "nfc_toggle"

    //keyboard
    const val MOVE_CURSOR_TO_END = "move_cursor_to_end"
    const val TOGGLE_KEYBOARD = "toggle_keyboard"
    const val SHOW_KEYBOARD = "show_keyboard"
    const val HIDE_KEYBOARD = "hide_keyboard"
    const val SHOW_KEYBOARD_PICKER = "show_keyboard_picker"
    const val SHOW_KEYBOARD_PICKER_ROOT = "show_keyboard_picker_root"

    //other
    const val SCREENSHOT = "screenshot"

    const val OPEN_ASSISTANT = "open_assistant"
    const val OPEN_CAMERA = "open_camera"
    const val LOCK_DEVICE = "lock_device"
    const val SECURE_LOCK_DEVICE = "secure_lock_device"
    const val CONSUME_KEY_EVENT = "consume_key_event"
    /**
     * REQUIRES ROOT
     */
    const val OPEN_MENU = "open_menu"

    const val CATEGORY_WIFI = "wifi"
    const val CATEGORY_BLUETOOTH = "bluetooth"
    const val CATEGORY_MOBILE_DATA = "mobile_data"
    const val CATEGORY_NAVIGATION = "navigation"
    const val CATEGORY_VOLUME = "volume"
    const val CATEGORY_SCREEN_ROTATION = "screen_rotation"
    const val CATEGORY_BRIGHTNESS = "brightness"
    const val CATEGORY_STATUS_BAR = "status_bar"
    const val CATEGORY_MEDIA = "media"
    const val CATEGORY_FLASHLIGHT = "flashlight"
    const val CATEGORY_NFC = "nfc"
    const val CATEGORY_KEYBOARD = "keyboard"
    const val CATEGORY_OTHER = "other"
}