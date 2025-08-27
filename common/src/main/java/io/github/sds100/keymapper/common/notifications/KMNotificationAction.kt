package io.github.sds100.keymapper.common.notifications

sealed class KMNotificationAction {
    enum class IntentAction {
        RESUME_KEY_MAPS,
        PAUSE_KEY_MAPS,
        PAIRING_CODE_REPLY,
        DISMISS_TOGGLE_KEY_MAPS_NOTIFICATION,
        STOP_ACCESSIBILITY_SERVICE,
        START_ACCESSIBILITY_SERVICE,
        RESTART_ACCESSIBILITY_SERVICE,
        TOGGLE_KEY_MAPPER_IME,
        SHOW_KEYBOARD
    }

    sealed class Broadcast(val intentAction: IntentAction) : KMNotificationAction() {
        data object ResumeKeyMaps : Broadcast(IntentAction.RESUME_KEY_MAPS)
        data object PauseKeyMaps : Broadcast(IntentAction.PAUSE_KEY_MAPS)
        data object DismissToggleKeyMapsNotification :
            Broadcast(IntentAction.DISMISS_TOGGLE_KEY_MAPS_NOTIFICATION)

        data object StopAccessibilityService : Broadcast(IntentAction.STOP_ACCESSIBILITY_SERVICE)
        data object StartAccessibilityService : Broadcast(IntentAction.START_ACCESSIBILITY_SERVICE)
        data object RestartAccessibilityService :
            Broadcast(IntentAction.RESTART_ACCESSIBILITY_SERVICE)

        data object TogglerKeyMapperIme : Broadcast(IntentAction.TOGGLE_KEY_MAPPER_IME)
        data object ShowKeyboard : Broadcast(IntentAction.SHOW_KEYBOARD)
    }

    sealed class RemoteInput(
        val key: String, val intentAction: IntentAction
    ) : KMNotificationAction() {

        data object PairingCode : RemoteInput(
            key = "pairing_code",
            intentAction = IntentAction.PAIRING_CODE_REPLY
        )
    }

    sealed class Activity() : KMNotificationAction() {
        data object AccessibilitySettings : Activity()
        data class MainActivity(val action: String? = null) : Activity()
    }
}