package io.github.sds100.keymapper.base

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.actions.CreateActionUseCase
import io.github.sds100.keymapper.base.actions.CreateActionUseCaseImpl
import io.github.sds100.keymapper.base.actions.TestActionUseCase
import io.github.sds100.keymapper.base.actions.TestActionUseCaseImpl
import io.github.sds100.keymapper.base.actions.keyevent.ConfigKeyEventUseCase
import io.github.sds100.keymapper.base.actions.keyevent.ConfigKeyEventUseCaseImpl
import io.github.sds100.keymapper.base.actions.sound.ChooseSoundFileUseCase
import io.github.sds100.keymapper.base.actions.sound.ChooseSoundFileUseCaseImpl
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCaseImpl
import io.github.sds100.keymapper.base.home.ShowHomeScreenAlertsUseCase
import io.github.sds100.keymapper.base.home.ShowHomeScreenAlertsUseCaseImpl
import io.github.sds100.keymapper.base.keymaps.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.base.keymaps.CreateKeyMapShortcutUseCaseImpl
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
import io.github.sds100.keymapper.base.system.apps.DisplayAppShortcutsUseCase
import io.github.sds100.keymapper.base.system.apps.DisplayAppShortcutsUseCaseImpl
import io.github.sds100.keymapper.base.system.apps.DisplayAppsUseCase
import io.github.sds100.keymapper.base.system.apps.DisplayAppsUseCaseImpl
import io.github.sds100.keymapper.base.system.bluetooth.ChooseBluetoothDeviceUseCase
import io.github.sds100.keymapper.base.system.bluetooth.ChooseBluetoothDeviceUseCaseImpl
import io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardUseCaseImpl

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

    @Binds
    @ViewModelScoped
    abstract fun bindChooseBluetoothDeviceUseCase(impl: ChooseBluetoothDeviceUseCaseImpl): ChooseBluetoothDeviceUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindChooseSoundFileUseCase(impl: ChooseSoundFileUseCaseImpl): ChooseSoundFileUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindConfigKeyEventUseCase(impl: ConfigKeyEventUseCaseImpl): ConfigKeyEventUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDisplayAppShortcutsUseCase(impl: DisplayAppShortcutsUseCaseImpl): DisplayAppShortcutsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDisplayAppsUseCase(impl: DisplayAppsUseCaseImpl): DisplayAppsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindTestActionUseCase(impl: TestActionUseCaseImpl): TestActionUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindCreateKeyMapShortcutUseCase(impl: CreateKeyMapShortcutUseCaseImpl): CreateKeyMapShortcutUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindSetupGuiKeyboardUseCase(impl: SetupGuiKeyboardUseCaseImpl): SetupGuiKeyboardUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindCreateActionUseCase(impl: CreateActionUseCaseImpl): CreateActionUseCase
}
