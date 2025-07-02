package io.github.sds100.keymapper.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepository
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.FloatingLayoutRepository
import io.github.sds100.keymapper.data.repositories.GroupRepository
import io.github.sds100.keymapper.data.repositories.KeyMapRepository
import io.github.sds100.keymapper.data.repositories.LogRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.data.repositories.RoomAccessibilityNodeRepository
import io.github.sds100.keymapper.data.repositories.RoomFloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.RoomFloatingLayoutRepository
import io.github.sds100.keymapper.data.repositories.RoomGroupRepository
import io.github.sds100.keymapper.data.repositories.RoomKeyMapRepository
import io.github.sds100.keymapper.data.repositories.RoomLogRepository
import io.github.sds100.keymapper.data.repositories.SettingsPreferenceRepository
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
