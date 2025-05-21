package io.github.sds100.keymapper.base

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCaseImpl
import io.github.sds100.keymapper.base.home.ShowHomeScreenAlertsUseCase
import io.github.sds100.keymapper.base.home.ShowHomeScreenAlertsUseCaseImpl
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCaseImpl
import io.github.sds100.keymapper.base.keymaps.ListKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.ListKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.logging.DisplayLogUseCase
import io.github.sds100.keymapper.base.logging.DisplayLogUseCaseImpl
import io.github.sds100.keymapper.base.settings.ConfigSettingsUseCase
import io.github.sds100.keymapper.base.settings.ConfigSettingsUseCaseImpl
import io.github.sds100.keymapper.base.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.base.sorting.SortKeyMapsUseCaseImpl

@Module
@InstallIn(ViewModelComponent::class)
abstract class BaseViewModelHiltModule {
    @Binds
    @ViewModelScoped
    abstract fun bindDisplayKeyMapUseCase(impl: DisplayKeyMapUseCaseImpl): DisplayKeyMapUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindListKeyMapsUseCase(impl: ListKeyMapsUseCaseImpl): ListKeyMapsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindBackupRestoreMappingsUseCase(impl: BackupRestoreMappingsUseCaseImpl): BackupRestoreMappingsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindShowHomeScreenAlertsUseCase(impl: ShowHomeScreenAlertsUseCaseImpl): ShowHomeScreenAlertsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindSortKeyMapsUseCase(impl: SortKeyMapsUseCaseImpl): SortKeyMapsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDisplayLogUseCase(impl: DisplayLogUseCaseImpl): DisplayLogUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindConfigSettingsUseCase(impl: ConfigSettingsUseCaseImpl): ConfigSettingsUseCase
}
