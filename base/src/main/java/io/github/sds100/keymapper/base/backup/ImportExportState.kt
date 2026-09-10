package io.github.sds100.keymapper.base.backup

sealed class ImportExportState {
    data object Idle : ImportExportState()
    data object Exporting : ImportExportState()
    data class FinishedExport(val uri: String) : ImportExportState()

    /**
     * The backup was written directly to the Downloads folder because there is no usable
     * system file picker to share it through (Android TV).
     */
    data class FinishedExportToDownloads(val fileName: String) : ImportExportState()

    data class ConfirmImport(val fileUri: String, val keyMapCount: Int) : ImportExportState()

    /**
     * Let the user pick a backup found in Downloads to import, for a device with no
     * usable system file picker (Android TV).
     */
    data class ChooseImportFileFromDownloads(
        val files: List<BackupFileListItem>,
        val canRequestFullAccess: Boolean,
    ) : ImportExportState()

    data object Importing : ImportExportState()
    data object FinishedImport : ImportExportState()
    data class Error(val error: String) : ImportExportState()
}

data class BackupFileListItem(val uri: String, val name: String)
