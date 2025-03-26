package io.github.sds100.keymapper.backup

sealed class ImportExportState {
    data object Idle : ImportExportState()
    data object Exporting : ImportExportState()
    data class FinishedExport(val uri: String) : ImportExportState()

    data class ConfirmImport(val fileUri: String, val keyMapCount: Int) : ImportExportState()
    data object Importing : ImportExportState()
    data object FinishedImport : ImportExportState()
    data class Error(val error: String) : ImportExportState()
}
