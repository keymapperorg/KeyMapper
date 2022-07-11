package io.github.sds100.keymapper.util

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Created by sds100 on 06/07/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UtilitiesModule {
    @Binds
    abstract fun bindUuidGenerator(impl: DefaultUuidGenerator): UuidGenerator
}