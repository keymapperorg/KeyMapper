package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.FileRepository
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.result.DownloadFailed
import io.github.sds100.keymapper.util.result.Failure
import io.github.sds100.keymapper.util.result.Result
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 04/04/2020.
 */

class HelpViewModel(private val repository: FileRepository) : ViewModel(), ProgressCallback {

    override val loadingContent = MutableLiveData(false)

    val markdownText = MutableLiveData<Result<String>>()

    init {
        refreshIfFailed()
    }

    fun refreshIfFailed() {
        if (markdownText.value == null || markdownText.value is Failure) {
            viewModelScope.launch {
                loadingContent.value = true

                markdownText.value = repository.getHelpMarkdown()

                loadingContent.value = false
            }
        }
    }

    class Factory(
        private val mRepository: FileRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            HelpViewModel(mRepository) as T
    }
}