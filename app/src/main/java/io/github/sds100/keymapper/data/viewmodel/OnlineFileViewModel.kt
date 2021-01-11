@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.repository.FileRepository
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.SSLHandshakeError
import io.github.sds100.keymapper.util.result.handle

/**
 * Created by sds100 on 04/04/2020.
 */

class OnlineFileViewModel(
    private val repository: FileRepository,
    private val fileUrl: String,
    private val alternateUrl: String? = null,
    val header: String) : ViewModel() {

    private val markdownResult = liveData {
        emit(Loading())

        emit(Data(repository.getFile(fileUrl)))
    }

    val markdownText = markdownResult.map { result ->
        result.mapData { data ->
            data.handle(
                onSuccess = {
                    it
                },
                onFailure = {
                    if (it is SSLHandshakeError) {
                        if (alternateUrl != null) {
                            _eventStream.value = OpenUrl(alternateUrl)
                        }
                    }

                    _eventStream.value = ShowErrorMessage(it)
                    _eventStream.value = CloseDialog()

                    ""
                }
            )
        }
    }

    private val _eventStream = LiveEvent<Event>()
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