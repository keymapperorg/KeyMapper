package io.github.sds100.keymapper.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by sds100 on 29/06/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    @Singleton
    abstract fun bindBackupManager(impl: BackupManagerImpl): BackupManager
    
    @Binds
    abstract fun bindBackupRestoreMappingsUseCase(impl: BackupRestoreMappingsUseCaseImpl):BackupRestoreMappingsUseCase
}