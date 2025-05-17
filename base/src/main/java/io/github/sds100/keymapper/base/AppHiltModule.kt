package io.github.sds100.keymapper.base

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.base.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.base.util.DispatcherProvider
import io.github.sds100.keymapper.common.BuildConfigProvider
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

    @Singleton
    @Provides
    fun provideBuildConfigProvider(): BuildConfigProvider = object : BuildConfigProvider {
        override val minApi: Int
            get() = BuildConfig.MIN_API
        override val maxApi: Int
            get() = BuildConfig.MAX_API
        override val packageName: String
            get() = BuildConfig.APPLICATION_ID
        override val version: String
            get() = BuildConfig.VERSION_NAME
        override val versionCode: Int
            get() = BuildConfig.VERSION_CODE
    }
}
