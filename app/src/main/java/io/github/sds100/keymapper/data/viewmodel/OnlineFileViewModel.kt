@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.data.FileRepository
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.result.Failure
import io.github.sds100.keymapper.util.result.SSLHandshakeError
import io.github.sds100.keymapper.util.result.handle

/**
 * Created by sds100 on 04/04/2020.
 */

class OnlineFileViewModel(
    private val mRepository: FileRepository,
    private val mFileUrl: String,
    private val mAlternateUrl: String? = null,
    val header: String) : ViewModel(), ProgressCallback {

    override val loadingContent = MutableLiveData(false)

    private val mMarkdownResult = liveData {
        loadingContent.value = true

        emit(mRepository.getFile(mFileUrl))

        loadingContent.value = false
    }

    val markdownText = mMarkdownResult.map { result ->
        result.handle(
            onSuccess = {
                it
            },
            onFailure = {
                if (it is SSLHandshakeError) {
                    if (mAlternateUrl != null) {
                        openUrlExternallyEvent.value = Event(mAlternateUrl)
                    }
                }

                showErrorEvent.value = Event(it)
                closeDialogEvent.value = Event(Unit)

                ""
            }
        )
    }

    val openUrlExternallyEvent = MutableLiveData<Event<String>>()
    val showErrorEvent = MutableLiveData<Event<Failure>>()
    val closeDialogEvent = MutableLiveData<Event<Unit>>()

    class Factory(
        private val mRepository: FileRepository,
        private val mFileUrl: String,
        private val mAlternateUrl: String? = null,
        private val mHeader: String
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            OnlineFileViewModel(mRepository, mFileUrl, mAlternateUrl, mHeader) as T
    }
}