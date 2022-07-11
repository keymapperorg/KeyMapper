package io.github.sds100.keymapper.actions

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.actions.keyevent.ConfigKeyEventUseCase
import io.github.sds100.keymapper.actions.keyevent.ConfigKeyEventUseCaseImpl
import io.github.sds100.keymapper.actions.sound.ChooseSoundFileUseCase
import io.github.sds100.keymapper.actions.sound.ChooseSoundFileUseCaseImpl
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.actions.sound.SoundsManagerImpl
import javax.inject.Singleton

/**
 * Created by sds100 on 28/06/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ActionsModule {
    @Binds
    @Singleton
    abstract fun bindSoundsManager(impl: SoundsManagerImpl): SoundsManager

    @Binds
    abstract fun bindCreateActionUseCase(impl: CreateActionUseCaseImpl): CreateActionUseCase

    @Binds
    abstract fun bindIsActionSupportedUseCase(impl: IsActionSupportedUseCaseImpl): IsActionSupportedUseCase

    @Binds
    abstract fun bindGetActionErrorUseCase(impl: GetActionErrorUseCaseImpl): GetActionErrorUseCase

    @Binds
    abstract fun bindChooseSoundFileUseCase(impl: ChooseSoundFileUseCaseImpl): ChooseSoundFileUseCase

    @Binds
    abstract fun bindTestActionUseCase(impl: TestActionUseCaseImpl): TestActionUseCase
    @Binds
    abstract fun bindConfigKeyEventActionUseCase(impl: ConfigKeyEventUseCaseImpl):ConfigKeyEventUseCase
    
}