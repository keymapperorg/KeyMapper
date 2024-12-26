package io.github.sds100.keymapper.util

import android.os.Parcelable
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed class ServiceEvent {

    @Serializable
    data class Ping(val key: String) : ServiceEvent()

    @Serializable
    data class Pong(val key: String) : ServiceEvent()

    @Parcelize
    @Serializable
    data class RecordedTriggerKey(
        val keyCode: Int,
        val device: InputDeviceInfo?,
        val detectionSource: KeyEventDetectionSource
    ) : ServiceEvent(),
        Parcelable

    @Serializable
    object StartRecordingTrigger : ServiceEvent()

    @Serializable
    object StopRecordingTrigger : ServiceEvent()

    @Serializable
    data class OnIncrementRecordTriggerTimer(val timeLeft: Int) : ServiceEvent()

    @Serializable
    object OnStoppedRecordingTrigger : ServiceEvent()

    @Serializable
    object OnHideKeyboardEvent : ServiceEvent()

    @Serializable
    object OnShowKeyboardEvent : ServiceEvent()

    @Serializable
    object HideKeyboard : ServiceEvent()

    @Serializable
    object ShowKeyboard : ServiceEvent()

    @Serializable
    data class TestAction(val action: ActionData) : ServiceEvent()

    @Serializable
    data class ChangeIme(val imeId: String) : ServiceEvent()

    @Serializable
    object DisableService : ServiceEvent()

    @Serializable
    object DismissLastNotification : ServiceEvent()

    @Serializable
    object DismissAllNotifications : ServiceEvent()

    @Serializable
    data class OnInputFocusChange(val isFocussed: Boolean) : ServiceEvent()

    @Serializable
    data class TriggerKeyMap(val uid: String) : ServiceEvent()
}
