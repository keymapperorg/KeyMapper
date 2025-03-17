package io.github.sds100.keymapper.backup

import io.github.sds100.keymapper.system.files.IFile
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 16/04/2021.
 */

class BackupRestoreMappingsUseCaseImpl(
    private val backupManager: BackupManager,
) : BackupRestoreMappingsUseCase {

    override val onAutomaticBackupResult: Flow<Result<*>> = backupManager.onAutomaticBackupResult

    override suspend fun backupEverything(): Result<IFile> = backupManager.backupEverything()

    override suspend fun restoreMappings(uri: String): Result<*> = backupManager.restore(uri)
}

interface BackupRestoreMappingsUseCase {
    val onAutomaticBackupResult: Flow<Result<*>>
    suspend fun backupEverything(): Result<IFile>
    suspend fun restoreMappings(uri: String): Result<*>
}
