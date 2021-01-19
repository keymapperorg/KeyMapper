package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.IGlobalPreferences
import io.github.sds100.keymapper.data.darkThemeMode
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.SetTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 18/01/21.
 */
class HomeViewModel(private val globalPreferences: IGlobalPreferences) : ViewModel() {

    private val _eventStream = LiveEvent<Event>()
    val eventStream: LiveData<Event> = _eventStream

    init {
        viewModelScope.launch {
            globalPreferences.darkThemeMode().collect {
                _eventStream.value = SetTheme(it)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val globalPreferences: IGlobalPreferences
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return HomeViewModel(globalPreferences) as T
        }
    }
}