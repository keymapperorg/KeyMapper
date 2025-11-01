package io.github.sds100.keymapper.base.trigger

import androidx.annotation.Keep

@Keep
enum class TriggerSetupShortcut {
    VOLUME,
    ASSISTANT,
    POWER,
    FINGERPRINT_GESTURE,
    KEYBOARD,
    MOUSE,
    GAMEPAD,
    OTHER,
    NOT_DETECTED,
    FLOATING_BUTTON_CUSTOM,
    FLOATING_BUTTON_LOCK_SCREEN,
}
