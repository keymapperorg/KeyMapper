package io.github.sds100.keymapper.system.accessibility

import kotlinx.serialization.Serializable

@Serializable
abstract class AccessibilityServiceEvent  {

    @Serializable
    data class Ping(val key: String) : AccessibilityServiceEvent()

    @Serializable
    data class Pong(val key: String) : AccessibilityServiceEvent()

    @Serializable
    data object OnHideKeyboardEvent : AccessibilityServiceEvent()

    @Serializable
    data object OnShowKeyboardEvent : AccessibilityServiceEvent()

    @Serializable
    data object HideKeyboard : AccessibilityServiceEvent()

    @Serializable
    data object ShowKeyboard : AccessibilityServiceEvent()

    @Serializable
    data class ChangeIme(val imeId: String) : AccessibilityServiceEvent()

    @Serializable
    data object DisableService : AccessibilityServiceEvent()

    @Serializable
    data class OnInputFocusChange(val isFocussed: Boolean) : AccessibilityServiceEvent()

    @Serializable
    data class EnableInputMethod(val imeId: String) : AccessibilityServiceEvent()

    @Serializable
    data class GlobalAction(val action: Int) : AccessibilityServiceEvent()
}
