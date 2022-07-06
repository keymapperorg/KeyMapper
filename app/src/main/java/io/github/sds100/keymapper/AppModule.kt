package io.github.sds100.keymapper

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import javax.inject.Singleton

/**
 * Created by sds100 on 28/06/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppCoroutineScope(): CoroutineScope {
        return MainScope()
    }

    @Provides
    @Singleton
    fun provideDispatchers(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }
}