package io.github.sds100.keymapper.base.backup

import io.github.sds100.keymapper.common.util.result.Result
import io.github.sds100.keymapper.common.util.result.Success
import io.github.sds100.keymapper.common.util.result.onFailure
import io.github.sds100.keymapper.common.util.result.then
import io.github.sds100.keymapper.system.files.FileAdapter
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class BackupRestoreMappingsUseCaseImpl(
    private val fileAdapter: FileAdapter,
    private val backupManager: BackupManager,
) : BackupRestoreMappingsUseCase {

    override val onAutomaticBackupResult: Flow<Result<*>> = backupManager.onAutomaticBackupResult

    override suspend fun backupEverything(): Result<String> {
        val fileName = BackupUtils.createBackupFileName()

        // Share in private files so the share sheet can show the file name. This is some quirk
        // of the storage access framework https://issuetracker.google.com/issues/268079113.
        // Saving it directly to Downloads with the MediaStore returns a content URI
        // that only contains a numerical ID, not the file name.
        return fileAdapter.getPrivateFile("${BackupManagerImpl.BACKUP_DIR}/$fileName").let { file ->
            file.createFile()
            backupManager.backupEverything(file)
            Success(fileAdapter.getPublicUriForPrivateFile(file))
        }
    }

    override suspend fun restoreKeyMaps(uri: String, restoreType: RestoreType): Result<*> {
        val file = fileAdapter.getFileFromUri(uri)
        return backupManager.restore(file, restoreType = restoreType)
            .onFailure { Timber.e(it.toString()) }
    }

    override suspend fun getKeyMapCountInBackup(uri: String): Result<Int> {
        val file = fileAdapter.getFileFromUri(uri)
        return backupManager.getBackupContent(file)
            .then { Success(it.keyMapList?.size ?: 0) }
            .onFailure { Timber.e(it.toString()) }
    }
}

interface BackupRestoreMappingsUseCase {
    val onAutomaticBackupResult: Flow<Result<*>>
    suspend fun backupEverything(): Result<String>
    suspend fun restoreKeyMaps(uri: String, restoreType: RestoreType): Result<*>

    suspend fun getKeyMapCountInBackup(uri: String): Result<Int>
}
