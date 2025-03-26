package io.github.sds100.keymapper.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RestoreKeyMapsViewModel(
    private val useCase: BackupRestoreMappingsUseCase,
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider {

    private val _importExportState = MutableStateFlow<ImportExportState>(ImportExportState.Idle)
    val importExportState: StateFlow<ImportExportState> = _importExportState.asStateFlow()

    fun onChooseImportFile(uri: String) {
        viewModelScope.launch {
            useCase.getKeyMapCountInBackup(uri).onSuccess {
                _importExportState.value = ImportExportState.ConfirmImport(uri, it)
            }.onFailure {
                _importExportState.value =
                    ImportExportState.Error(it.getFullMessage(this@RestoreKeyMapsViewModel))
            }
        }
    }

    fun onConfirmImport(restoreType: RestoreType) {
        val state = _importExportState.value as? ImportExportState.ConfirmImport
        state ?: return

        _importExportState.value = ImportExportState.Importing

        viewModelScope.launch {
            useCase.restoreKeyMaps(state.fileUri, restoreType).onSuccess {
                _importExportState.value = ImportExportState.FinishedImport
            }.onFailure {
                _importExportState.value =
                    ImportExportState.Error(it.getFullMessage(this@RestoreKeyMapsViewModel))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val useCase: BackupRestoreMappingsUseCase,
        private val resourceProvider: ResourceProvider,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T = RestoreKeyMapsViewModel(useCase, resourceProvider) as T
    }
}
