package io.github.sds100.keymapper.backup

import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.then
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Created by sds100 on 16/04/2021.
 */

class BackupRestoreMappingsUseCaseImpl(
    private val fileAdapter: FileAdapter,
    private val backupManager: BackupManager,
) : BackupRestoreMappingsUseCase {

    override val onAutomaticBackupResult: Flow<Result<*>> = backupManager.onAutomaticBackupResult

    override suspend fun backupEverything(): Result<String> {
        val fileName = BackupUtils.createBackupFileName()

        return fileAdapter.openDownloadsFile(fileName, FileUtils.MIME_TYPE_ZIP).then { file ->
            backupManager.backupEverything(file)
            Success(file.uri)
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
