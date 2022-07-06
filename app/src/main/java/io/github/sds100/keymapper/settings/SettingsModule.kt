package io.github.sds100.keymapper.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Created by sds100 on 29/06/2022.
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class SettingsModule {
    @Binds
    abstract fun bindConfigSettingsUseCase(impl: ConfigSettingsUseCaseImpl): ConfigSettingsUseCase
}