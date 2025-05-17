package io.github.sds100.keymapper.base.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.common.util.result.onFailure
import io.github.sds100.keymapper.common.util.result.onSuccess
import io.github.sds100.keymapper.base.util.getFullMessage
import io.github.sds100.keymapper.base.util.ui.ResourceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RestoreKeyMapsViewModel @Inject constructor(
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
}
