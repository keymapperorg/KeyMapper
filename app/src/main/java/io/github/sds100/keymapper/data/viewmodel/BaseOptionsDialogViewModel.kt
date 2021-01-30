@file:Suppress("UNCHECKED_CAST")

package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.LiveData
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.model.options.BaseOptions
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.OpenUrlRes
import io.github.sds100.keymapper.util.SaveEvent

/**
 * Created by sds100 on 28/11/20.
 */

abstract class BaseOptionsDialogViewModel<O : BaseOptions<*>> : BaseOptionsViewModel<O>() {

    private val _eventStream = LiveEvent<Event>()
    val eventStream: LiveData<Event> = _eventStream

    fun save() {
        options.value?.let {
            _eventStream.value = SaveEvent(it)
        }
    }

    fun openHelpUrl() {
        _eventStream.value = OpenUrlRes(helpUrl)
    }

    abstract val helpUrl: Int
}