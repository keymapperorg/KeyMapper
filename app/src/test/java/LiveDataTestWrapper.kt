import androidx.lifecycle.LiveData

/**
 * Created by sds100 on 23/12/20.
 */

class LiveDataTestWrapper<T>(liveData: LiveData<T>) {
    private val _history = mutableListOf<T>()
    val history
        get() = _history.toList()

    val historyCount
        get() = history.size

    init {
        liveData.observeForever {
            _history.add(it)
        }
    }

    fun latestValue() = history.last()

    fun reset() {
        _history.clear()
    }
}