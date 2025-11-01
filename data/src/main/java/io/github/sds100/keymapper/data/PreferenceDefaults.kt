package io.github.sds100.keymapper.data

object PreferenceDefaults {
    const val DARK_THEME = "2"

    const val SHOW_TOAST_WHEN_AUTO_CHANGE_IME = false
    const val CHANGE_IME_ON_INPUT_FOCUS = false

    const val FORCE_VIBRATE = false
    const val LONG_PRESS_DELAY = 500
    const val DOUBLE_PRESS_DELAY = 300
    const val VIBRATION_DURATION = 200
    const val REPEAT_DELAY = 400
    const val REPEAT_RATE = 50
    const val SEQUENCE_TRIGGER_TIMEOUT = 1000
    const val HOLD_DOWN_DURATION = 1000

    const val PRO_MODE_INTERACTIVE_SETUP_ASSISTANT = true

    // Enable this by default so that key maps will still work for root users
    // who upgrade to version 4.0.
    const val PRO_MODE_AUTOSTART_BOOT = true

    // It is false by default and the first time they turn on the system bridge,
    // the preference will be set to true.
    const val KEY_EVENT_ACTIONS_USE_SYSTEM_BRIDGE = false
}
