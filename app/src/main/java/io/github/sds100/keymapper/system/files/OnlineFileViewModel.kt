@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.github.sds100.keymapper.system.files

import androidx.lifecycle.*
import io.github.sds100.keymapper.util.*

/**
 * Created by sds100 on 04/04/2020.
 */

//TODO use FileAdapter
class OnlineFileViewModel(
    private val repository: FileRepository,
    private val fileUrl: String,
    private val alternateUrl: String? = null,
    val header: String) : ViewModel() {

    private val _eventStream = MutableLiveData<Event>()
    val eventStream: LiveData<Event> = _eventStream

    class Factory(
        private val repository: FileRepository,
        private val fileUrl: String,
        private val alternateUrl: String? = null,
        private val header: String
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            OnlineFileViewModel(repository, fileUrl, alternateUrl, header) as T
    }
}