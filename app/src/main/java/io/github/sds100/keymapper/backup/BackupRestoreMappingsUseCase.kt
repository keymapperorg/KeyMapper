package io.github.sds100.keymapper.backup

import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.then
import kotlinx.coroutines.flow.Flow

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

        return fileAdapter.openDownloadsFile(fileName, FileUtils.MIME_TYPE_ZIP).then {
            backupManager.backupEverything(it)
        }.then { Success(it.uri) }
    }

    override suspend fun restoreKeyMaps(uri: String): Result<*> = backupManager.restore(uri)
}

interface BackupRestoreMappingsUseCase {
    val onAutomaticBackupResult: Flow<Result<*>>
    suspend fun backupEverything(): Result<String>
    suspend fun restoreKeyMaps(uri: String): Result<*>
}
