package io.github.sds100.keymapper.util.delegate

import com.hadilq.liveevent.LiveEvent

/**
 * Created by sds100 on 23/12/20.
 */

class LiveEventTestWrapper<T>(liveEvent: LiveEvent<T>) {
    private val _history = mutableListOf<T>()
    val history
        get() = _history.toList()

    val historyCount
        get() = history.size

    init {
        liveEvent.observeForever {
            _history.add(it)
        }
    }

    fun reset() {
        _history.clear()
    }
}