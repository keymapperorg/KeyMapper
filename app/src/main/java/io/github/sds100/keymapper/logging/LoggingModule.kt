package io.github.sds100.keymapper.logging

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Created by sds100 on 07/07/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {

    @Binds
    abstract fun bindDisplayLogUseCase(impl: DisplayLogUseCaseImpl): DisplayLogUseCase
}