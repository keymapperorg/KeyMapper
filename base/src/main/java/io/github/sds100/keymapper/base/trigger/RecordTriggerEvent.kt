package io.github.sds100.keymapper.base.trigger

import android.os.Parcelable
import io.github.sds100.keymapper.common.utils.InputDeviceInfo
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

sealed class RecordTriggerEvent : AccessibilityServiceEvent() {
    @Parcelize
    @Serializable
    data class RecordedTriggerKey(
        val keyCode: Int,
        val device: InputDeviceInfo?,
        val detectionSource: KeyEventDetectionSource,
    ) : RecordTriggerEvent(),
        Parcelable

    @Serializable
    data object StartRecordingTrigger : RecordTriggerEvent()

    @Serializable
    data object StopRecordingTrigger : RecordTriggerEvent()

    @Serializable
    data class OnIncrementRecordTriggerTimer(val timeLeft: Int) : RecordTriggerEvent()

    @Serializable
    data object OnStoppedRecordingTrigger : RecordTriggerEvent()
}
