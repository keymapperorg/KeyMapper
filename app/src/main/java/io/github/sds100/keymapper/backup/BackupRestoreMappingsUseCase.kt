package io.github.sds100.keymapper.backup

import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Created by sds100 on 16/04/2021.
 */

class BackupRestoreMappingsUseCaseImpl @Inject constructor(
    private val backupManager: BackupManager,
) : BackupRestoreMappingsUseCase {

    override val onAutomaticBackupResult: Flow<Result<*>> = backupManager.onAutomaticBackupResult

    override suspend fun backupAllMappings(uri: String): Result<String> {
        return backupManager.backupMappings(uri)
    }

    override suspend fun restoreMappings(uri: String): Result<*> {
        return backupManager.restoreMappings(uri)
    }
}

interface BackupRestoreMappingsUseCase {
    val onAutomaticBackupResult: Flow<Result<*>>
    suspend fun backupAllMappings(uri: String): Result<String>
    suspend fun restoreMappings(uri: String): Result<*>
}