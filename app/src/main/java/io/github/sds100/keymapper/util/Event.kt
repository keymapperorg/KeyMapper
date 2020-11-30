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

import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.options.BaseOptions
import io.github.sds100.keymapper.data.model.options.TriggerKeyOptions
import io.github.sds100.keymapper.util.result.Failure
import java.io.Serializable


sealed class SealedEvent

abstract class MessageEvent(@StringRes val textRes: Int) : SealedEvent()

class FixFailure(val failure: Failure) : SealedEvent()

class OkDialog(@StringRes val message: Int, val onOk: () -> Unit) : SealedEvent()
class EnableAccessibilityServicePrompt : SealedEvent()

//trigger
class BuildTriggerKeyModels(val source: List<Trigger.Key>) : SealedEvent()
class EditTriggerKeyOptions(val options: TriggerKeyOptions) : SealedEvent()
class EnableCapsLockKeyboardLayoutPrompt : SealedEvent()
class StartRecordingTriggerInService : SealedEvent()
class StopRecordingTriggerInService : SealedEvent()

//action list
class BuildActionListModels(val source: List<Action>) : SealedEvent()
class TestAction(val action: Action) : SealedEvent()
class EditActionOptions(val options: BaseOptions<Action>) : SealedEvent()

//constraint list
class DuplicateConstraints : MessageEvent(R.string.error_duplicate_constraint)
class BuildConstraintListModels(val source: List<Constraint>) : SealedEvent()


/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
open class Event<out T>(private val content: T) : Serializable {

    @Suppress("MemberVisibilityCanBePrivate")
    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun handled() {
        hasBeenHandled = true
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}

/**
 * An [Observer] for [Event]s, simplifying the pattern of checking if the [Event]'s content has
 * already been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [Event]'s contents has not been handled.
 */
class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>?) {
        event?.getContentIfNotHandled()?.let {
            onEventUnhandledContent(it)
        }
    }
}
