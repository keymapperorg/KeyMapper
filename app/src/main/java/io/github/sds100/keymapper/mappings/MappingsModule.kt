package io.github.sds100.keymapper.mappings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent

/**
 * Created by sds100 on 06/07/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MappingsModule {
    @Binds
    abstract fun bindPauseMappingsUseCase(impl: PauseMappingsUseCaseImpl): PauseMappingsUseCase

    @Binds
    abstract fun bindDisplaySimpleMappingUseCase(impl: DisplaySimpleMappingUseCaseImpl):DisplaySimpleMappingUseCase
}