package io.github.sds100.keymapper.actions

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.actions.sound.SoundsManagerImpl

/**
 * Created by sds100 on 28/06/2022.
 */
@Module
@InstallIn(ViewModelComponent::class, SingletonComponent::class)
abstract class ActionsModule {
    @Binds
    abstract fun bindSoundsManager(impl: SoundsManagerImpl): SoundsManager
}