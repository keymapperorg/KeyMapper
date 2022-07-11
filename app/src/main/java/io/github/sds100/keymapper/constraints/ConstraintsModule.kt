package io.github.sds100.keymapper.constraints

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Created by sds100 on 06/07/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConstraintsModule {

    @Binds
    abstract fun bindCreateConstraintUseCase(impl: CreateConstraintUseCaseImpl): CreateConstraintUseCase

    @Binds
    abstract fun bindGetConstraintErrorUseCase(impl: GetConstraintErrorUseCaseImpl): GetConstraintErrorUseCase
}