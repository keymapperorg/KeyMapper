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
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed class Event

@Serializable
data class Ping(val key: String) : Event()
@Serializable
data class Pong(val key: String) : Event()

@Parcelize
@Serializable
data class RecordedTriggerKeyEvent(
    val keyCode: Int,
    val deviceName: String,
    val deviceDescriptor: String,
    val isExternal: Boolean
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
object HideKeyboardEvent: Event()

@Serializable
object ShowKeyboardEvent: Event()

@Serializable
data class TestActionEvent(val action: ActionData): Event()

@Serializable
data class ChangeIme(val imeId: String): Event()