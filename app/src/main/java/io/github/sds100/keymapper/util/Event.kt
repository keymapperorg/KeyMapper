/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.sds100.keymapper.util

import android.os.Parcelable
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.ui.UiElementInfo
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed class Event {

    @Serializable
    data class Ping(val key: String) : Event()

    @Serializable
    data class Pong(val key: String) : Event()

    @Parcelize
    @Serializable
    data class RecordedTriggerKey(
        val keyCode: Int,
        val device: InputDeviceInfo?
    ) : Event(), Parcelable

    @Serializable
    object StartRecordingTrigger : Event()

    @Serializable
    object StopRecordingTrigger : Event()

    @Serializable
    data class OnIncrementRecordTriggerTimer(val timeLeft: Int) : Event()

    @Serializable
    object OnStoppedRecordingTrigger : Event()

    @Serializable
    object OnHideKeyboardEvent : Event()

    @Serializable
    object OnShowKeyboardEvent : Event()

    @Serializable
    object HideKeyboard : Event()

    @Serializable
    object ShowKeyboard : Event()

    @Serializable
    data class TestAction(val action: ActionData) : Event()

    @Serializable
    data class ChangeIme(val imeId: String) : Event()

    @Serializable
    object DisableService : Event()

    @Serializable
    object DismissLastNotification : Event()

    @Serializable
    object DismissAllNotifications : Event()

    @Serializable
    data class OnInputFocusChange(val isFocussed: Boolean) : Event()

    @Serializable
    object StartRecordingUiElements: Event()

    @Serializable
    object StopRecordingUiElements: Event()

    @Serializable
    data class OnIncrementRecordUiElementsTimer(val timeLeft: Int) : Event()

    @Serializable
    object OnStoppedRecordingUiElements : Event()

    data class OnRecordUiElement(val element: UiElementInfo) : Event()
}