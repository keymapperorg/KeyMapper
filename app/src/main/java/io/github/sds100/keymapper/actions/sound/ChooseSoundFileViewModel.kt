package io.github.sds100.keymapper.actions.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 31/03/2020.
 */
class ChooseSoundFileViewModel(
    resourceProvider: ResourceProvider
) : ViewModel(), PopupViewModel by PopupViewModelImpl(), ResourceProvider by resourceProvider {

    private val _chooseSoundFile = MutableSharedFlow<Unit>()
    val chooseSoundFile = _chooseSoundFile.asSharedFlow()

    private val _returnResult = MutableSharedFlow<ChooseSoundFileResult>()
    val returnResult = _returnResult.asSharedFlow()

    fun onChooseSoundFileButtonClick() {
        viewModelScope.launch {
            _chooseSoundFile.emit(Unit)
        }
    }

    fun onChooseSoundFile(uri: String) {
        viewModelScope.launch {
            val dialog = PopupUi.Text(
                hint = getString(R.string.hint_sound_file_description),
                allowEmpty = false
            )

            val result = showPopup("file_description", dialog)

            result ?: return@launch

            _returnResult.emit(
                ChooseSoundFileResult(
                    uri = uri,
                    description = result.text
                )
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChooseSoundFileViewModel(resourceProvider) as T
        }
    }
}