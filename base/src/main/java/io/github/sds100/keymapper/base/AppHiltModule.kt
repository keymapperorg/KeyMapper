package io.github.sds100.keymapper.base

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.base.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.base.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppHiltModule {
    @Singleton
    @Provides
    fun provideCoroutineScope(): CoroutineScope = MainScope()

    @Provides
    @Singleton
    fun provideDispatchers(): DispatcherProvider = DefaultDispatcherProvider()
}
