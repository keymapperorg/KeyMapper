package io.github.sds100.keymapper.system

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.system.lock.AndroidLockScreenAdapter
import io.github.sds100.keymapper.system.lock.LockScreenAdapter
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemHiltModule {
    @Singleton
    @Binds
    abstract fun provideLockscreenAdapter(impl: AndroidLockScreenAdapter): LockScreenAdapter
}
