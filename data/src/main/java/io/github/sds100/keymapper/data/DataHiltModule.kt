package io.github.sds100.keymapper.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.data.repositories.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataHiltModule {
    @Singleton
    @Binds
    abstract fun providePreferenceRepository(impl: SettingsPreferenceRepository): PreferenceRepository

    @Singleton
    @Binds
    abstract fun provideGroupRepository(impl: RoomGroupRepository): GroupRepository

    @Singleton
    @Binds
    abstract fun provideKeyMapRepository(impl: RoomKeyMapRepository): KeyMapRepository

    @Singleton
    @Binds
    abstract fun provideAccessibilityNodeRepository(impl: RoomAccessibilityNodeRepository): AccessibilityNodeRepository

    @Singleton
    @Binds
    abstract fun provideLogRepository(impl: RoomLogRepository): LogRepository

    @Singleton
    @Binds
    abstract fun provideFloatingButtonRepository(impl: RoomFloatingButtonRepository): FloatingButtonRepository

    @Singleton
    @Binds
    abstract fun provideFloatingLayoutRepository(impl: RoomFloatingLayoutRepository): FloatingLayoutRepository
} 