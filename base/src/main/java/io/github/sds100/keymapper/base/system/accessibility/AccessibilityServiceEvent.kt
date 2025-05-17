package io.github.sds100.keymapper.base.system.accessibility

import android.os.Parcelable
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.mapping.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.system.accessibility.RecordAccessibilityNodeState
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.trigger.KeyEventDetectionSource
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed class AccessibilityServiceEvent {

    @Serializable
    data class Ping(val key: String) : AccessibilityServiceEvent()

    @Serializable
    data class Pong(val key: String) : AccessibilityServiceEvent()

    @Parcelize
    @Serializable
    data class RecordedTriggerKey(
        val keyCode: Int,
        val device: InputDeviceInfo?,
        val detectionSource: KeyEventDetectionSource,
    ) : AccessibilityServiceEvent(),
        Parcelable

    @Serializable
    object StartRecordingTrigger : AccessibilityServiceEvent()

    @Serializable
    object StopRecordingTrigger : AccessibilityServiceEvent()

    @Serializable
    data class OnIncrementRecordTriggerTimer(val timeLeft: Int) : AccessibilityServiceEvent()

    @Serializable
    object OnStoppedRecordingTrigger : AccessibilityServiceEvent()

    @Serializable
    object OnHideKeyboardEvent : AccessibilityServiceEvent()

    @Serializable
    object OnShowKeyboardEvent : AccessibilityServiceEvent()

    @Serializable
    object HideKeyboard : AccessibilityServiceEvent()

    @Serializable
    object ShowKeyboard : AccessibilityServiceEvent()

    @Serializable
    data class TestAction(val action: ActionData) : AccessibilityServiceEvent()

    @Serializable
    data class ChangeIme(val imeId: String) : AccessibilityServiceEvent()

    @Serializable
    object DisableService : AccessibilityServiceEvent()

    @Serializable
    object DismissLastNotification : AccessibilityServiceEvent()

    @Serializable
    object DismissAllNotifications : AccessibilityServiceEvent()

    @Serializable
    data class OnInputFocusChange(val isFocussed: Boolean) : AccessibilityServiceEvent()

    @Serializable
    data class TriggerKeyMap(val uid: String) : AccessibilityServiceEvent()

    @Serializable
    data class EnableInputMethod(val imeId: String) : AccessibilityServiceEvent()

    @Serializable
    data object StartRecordingNodes : AccessibilityServiceEvent()

    @Serializable
    data object StopRecordingNodes : AccessibilityServiceEvent()

    @Serializable
    data class OnRecordNodeStateChanged(val state: RecordAccessibilityNodeState) : AccessibilityServiceEvent()
}
