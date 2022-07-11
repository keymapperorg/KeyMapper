package io.github.sds100.keymapper

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.ResourceProviderImpl
import javax.inject.Singleton

/**
 * Created by sds100 on 06/07/2022.
 */

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {
    @Binds
    @Singleton
    abstract fun bindResourceProvider(impl: ResourceProviderImpl): ResourceProvider
}