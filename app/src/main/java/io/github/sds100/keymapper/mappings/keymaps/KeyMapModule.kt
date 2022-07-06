package io.github.sds100.keymapper.mappings.keymaps

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.mappings.PauseMappingsUseCaseImpl

/**
 * Created by sds100 on 06/07/2022.
 */
@Module
@InstallIn(ViewModelComponent::class, SingletonComponent::class)
abstract class KeyMapModule {
    @Binds
    abstract fun bindPauseMappingsUseCase(impl: PauseMappingsUseCaseImpl): PauseMappingsUseCase
}