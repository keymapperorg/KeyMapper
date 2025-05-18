package io.github.sds100.keymapper.base

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.base.system.notifications.AndroidNotificationAdapter
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProviderImpl
import io.github.sds100.keymapper.system.notifications.NotificationAdapter
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BaseHiltModule {
    @Singleton
    @Binds
    abstract fun provideNotificationAdapter(impl: AndroidNotificationAdapter): NotificationAdapter

    @Singleton
    @Binds
    abstract fun provideResourceProvider(impl: ResourceProviderImpl): ResourceProvider
}
