package io.github.sds100.keymapper.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.data.repositories.SettingsPreferenceRepository
import javax.inject.Singleton

/**
 * Created by sds100 on 06/07/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindings {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(imple: SettingsPreferenceRepository): PreferenceRepository
}