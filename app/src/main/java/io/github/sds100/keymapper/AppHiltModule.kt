package io.github.sds100.keymapper

import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.base.purchasing.PurchasingManager
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.KeyMapperClassProvider
import io.github.sds100.keymapper.common.utils.DefaultDispatcherProvider
import io.github.sds100.keymapper.common.utils.DispatcherProvider
import io.github.sds100.keymapper.purchasing.PurchasingManagerImpl
import io.github.sds100.keymapper.system.accessibility.MyAccessibilityService
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

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
            get() = Build.VERSION_CODES.O
        override val maxApi: Int
            get() = 1000
        override val packageName: String
            get() = BuildConfig.APPLICATION_ID
        override val version: String
            get() = BuildConfig.VERSION_NAME
        override val versionCode: Int
            get() = BuildConfig.VERSION_CODE
        override val sdkInt: Int
            get() = Build.VERSION.SDK_INT

        // Android TV users predominantly sideload this FOSS build rather than
        // install from Google Play, which restricts this permission to
        // file-manager apps, so it's safe to request it here.
        override val canRequestAllFilesAccess: Boolean
            get() = true
    }

    @Singleton
    @Provides
    fun provideClassProvider(): KeyMapperClassProvider = object : KeyMapperClassProvider {
        override fun getMainActivity(): Class<*> {
            return MainActivity::class.java
        }

        override fun getAccessibilityService(): Class<*> {
            return MyAccessibilityService::class.java
        }
    }

    @Provides
    @Singleton
    fun providePurchasingManager(): PurchasingManager = PurchasingManagerImpl()
}
