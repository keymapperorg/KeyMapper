package io.github.sds100.keymapper.mappings.keymaps

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import io.github.sds100.keymapper.logging.DisplayLogUseCase
import io.github.sds100.keymapper.logging.DisplayLogUseCaseImpl
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerUseCaseImpl

/**
 * Created by sds100 on 06/07/2022.
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class KeyMapModule {
    @Binds
    abstract fun bindListKeyMapsUseCase(impl: ListKeyMapsUseCaseImpl): ListKeyMapsUseCase

    @Binds
    abstract fun bindDisplayKeyMapUseCase(impl: DisplayKeyMapUseCaseImpl): DisplayKeyMapUseCase

    @Binds
    abstract fun bindConfigKeyMapUseCase(impl: ConfigKeyMapUseCaseImpl): ConfigKeyMapUseCase

    @Binds
    abstract fun bindRecordTriggerUseCase(impl: RecordTriggerUseCaseImpl): RecordTriggerUseCase

    @Binds
    abstract fun bindCreateKeyMapShortcutUseCase(impl: CreateKeyMapShortcutUseCaseImpl): CreateKeyMapShortcutUseCase
}