package io.github.sds100.keymapper.system.accessibility

import android.os.Build
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import kotlinx.serialization.Serializable

@Serializable
abstract class AccessibilityServiceEvent {

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

    @RequiresApi(Build.VERSION_CODES.R)
    @Serializable
    data class ChangeIme(val imeId: String) : AccessibilityServiceEvent()

    @Serializable
    data object DisableService : AccessibilityServiceEvent()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Serializable
    data class EnableInputMethod(val imeId: String) : AccessibilityServiceEvent()

    @Serializable
    data class GlobalAction(val action: Int) : AccessibilityServiceEvent()

    data class OnKeyMapperImeStartInput(val attribute: EditorInfo, val restarting: Boolean) :
        AccessibilityServiceEvent()
}
