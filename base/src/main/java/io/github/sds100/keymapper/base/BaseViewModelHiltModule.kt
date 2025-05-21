package io.github.sds100.keymapper.base

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCaseImpl
import io.github.sds100.keymapper.base.keymaps.ListKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.ListKeyMapsUseCaseImpl

@Module
@InstallIn(ViewModelComponent::class)
abstract class BaseViewModelHiltModule {
    @Binds
    abstract fun bindDisplayKeyMapUseCase(impl: DisplayKeyMapUseCaseImpl): DisplayKeyMapUseCase

    @Binds
    abstract fun bindListKeyMapsUseCase(impl: ListKeyMapsUseCaseImpl): ListKeyMapsUseCase
}
