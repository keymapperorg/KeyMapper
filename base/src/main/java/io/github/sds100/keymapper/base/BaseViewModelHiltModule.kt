package io.github.sds100.keymapper.base

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.actions.ConfigActionsUseCase
import io.github.sds100.keymapper.base.actions.ConfigActionsUseCaseImpl
import io.github.sds100.keymapper.base.actions.CreateActionUseCase
import io.github.sds100.keymapper.base.actions.CreateActionUseCaseImpl
import io.github.sds100.keymapper.base.actions.DisplayActionUseCase
import io.github.sds100.keymapper.base.actions.TestActionUseCase
import io.github.sds100.keymapper.base.actions.TestActionUseCaseImpl
import io.github.sds100.keymapper.base.actions.keyevent.ConfigKeyEventUseCase
import io.github.sds100.keymapper.base.actions.keyevent.ConfigKeyEventUseCaseImpl
import io.github.sds100.keymapper.base.actions.keyevent.FixKeyEventActionDelegate
import io.github.sds100.keymapper.base.actions.keyevent.FixKeyEventActionDelegateImpl
import io.github.sds100.keymapper.base.actions.sound.ChooseSoundFileUseCase
import io.github.sds100.keymapper.base.actions.sound.ChooseSoundFileUseCaseImpl
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCaseImpl
import io.github.sds100.keymapper.base.constraints.ConfigConstraintsUseCase
import io.github.sds100.keymapper.base.constraints.ConfigConstraintsUseCaseImpl
import io.github.sds100.keymapper.base.constraints.CreateConstraintUseCase
import io.github.sds100.keymapper.base.constraints.CreateConstraintUseCaseImpl
import io.github.sds100.keymapper.base.constraints.DisplayConstraintUseCase
import io.github.sds100.keymapper.base.expertmode.ExpertModeSetupDelegateImpl
import io.github.sds100.keymapper.base.expertmode.SystemBridgeSetupDelegate
import io.github.sds100.keymapper.base.expertmode.SystemBridgeSetupUseCase
import io.github.sds100.keymapper.base.expertmode.SystemBridgeSetupUseCaseImpl
import io.github.sds100.keymapper.base.home.ListKeyMapsUseCase
import io.github.sds100.keymapper.base.home.ListKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.home.ShowHomeScreenAlertsUseCase
import io.github.sds100.keymapper.base.home.ShowHomeScreenAlertsUseCaseImpl
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCaseImpl
import io.github.sds100.keymapper.base.logging.DisplayLogUseCase
import io.github.sds100.keymapper.base.logging.DisplayLogUseCaseImpl
import io.github.sds100.keymapper.base.logging.ShareLogcatUseCase
import io.github.sds100.keymapper.base.logging.ShareLogcatUseCaseImpl
import io.github.sds100.keymapper.base.onboarding.OnboardingTipDelegate
import io.github.sds100.keymapper.base.onboarding.OnboardingTipDelegateImpl
import io.github.sds100.keymapper.base.settings.ConfigSettingsUseCase
import io.github.sds100.keymapper.base.settings.ConfigSettingsUseCaseImpl
import io.github.sds100.keymapper.base.shortcuts.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.base.shortcuts.CreateKeyMapShortcutUseCaseImpl
import io.github.sds100.keymapper.base.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.base.sorting.SortKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.system.apps.DisplayAppShortcutsUseCase
import io.github.sds100.keymapper.base.system.apps.DisplayAppShortcutsUseCaseImpl
import io.github.sds100.keymapper.base.system.apps.DisplayAppsUseCase
import io.github.sds100.keymapper.base.system.apps.DisplayAppsUseCaseImpl
import io.github.sds100.keymapper.base.system.bluetooth.ChooseBluetoothDeviceUseCase
import io.github.sds100.keymapper.base.system.bluetooth.ChooseBluetoothDeviceUseCaseImpl
import io.github.sds100.keymapper.base.trigger.ConfigTriggerUseCase
import io.github.sds100.keymapper.base.trigger.ConfigTriggerUseCaseImpl
import io.github.sds100.keymapper.base.trigger.SetupInputMethodUseCase
import io.github.sds100.keymapper.base.trigger.SetupInputMethodUseCaseImpl
import io.github.sds100.keymapper.base.trigger.TriggerSetupDelegate
import io.github.sds100.keymapper.base.trigger.TriggerSetupDelegateImpl

@Module
@InstallIn(ViewModelComponent::class)
abstract class BaseViewModelHiltModule {
    @Binds
    @ViewModelScoped
    abstract fun bindDisplayKeyMapUseCase(impl: DisplayKeyMapUseCaseImpl): DisplayKeyMapUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDisplayActionUseCase(impl: DisplayKeyMapUseCaseImpl): DisplayActionUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDisplayConstraintUseCase(
        impl: DisplayKeyMapUseCaseImpl,
    ): DisplayConstraintUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindListKeyMapsUseCase(impl: ListKeyMapsUseCaseImpl): ListKeyMapsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindBackupRestoreMappingsUseCase(
        impl: BackupRestoreMappingsUseCaseImpl,
    ): BackupRestoreMappingsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindShowHomeScreenAlertsUseCase(
        impl: ShowHomeScreenAlertsUseCaseImpl,
    ): ShowHomeScreenAlertsUseCase

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
    abstract fun bindChooseBluetoothDeviceUseCase(
        impl: ChooseBluetoothDeviceUseCaseImpl,
    ): ChooseBluetoothDeviceUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindChooseSoundFileUseCase(
        impl: ChooseSoundFileUseCaseImpl,
    ): ChooseSoundFileUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindConfigKeyEventUseCase(impl: ConfigKeyEventUseCaseImpl): ConfigKeyEventUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDisplayAppShortcutsUseCase(
        impl: DisplayAppShortcutsUseCaseImpl,
    ): DisplayAppShortcutsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDisplayAppsUseCase(impl: DisplayAppsUseCaseImpl): DisplayAppsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindTestActionUseCase(impl: TestActionUseCaseImpl): TestActionUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindCreateKeyMapShortcutUseCase(
        impl: CreateKeyMapShortcutUseCaseImpl,
    ): CreateKeyMapShortcutUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindCreateActionUseCase(impl: CreateActionUseCaseImpl): CreateActionUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindCreateConstraintUseCase(
        impl: CreateConstraintUseCaseImpl,
    ): CreateConstraintUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindExpertModeSetupUseCase(
        impl: SystemBridgeSetupUseCaseImpl,
    ): SystemBridgeSetupUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindConfigConstraintsUseCase(
        impl: ConfigConstraintsUseCaseImpl,
    ): ConfigConstraintsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindConfigActionsUseCase(impl: ConfigActionsUseCaseImpl): ConfigActionsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindConfigTriggerUseCase(impl: ConfigTriggerUseCaseImpl): ConfigTriggerUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindTriggerSetupDelegate(impl: TriggerSetupDelegateImpl): TriggerSetupDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindSetupInputMethodUseCase(
        impl: SetupInputMethodUseCaseImpl,
    ): SetupInputMethodUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindShareLogcatUseCase(impl: ShareLogcatUseCaseImpl): ShareLogcatUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindOnboardingTipDelegate(impl: OnboardingTipDelegateImpl): OnboardingTipDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindFixKeyEventActionDelegate(
        impl: FixKeyEventActionDelegateImpl,
    ): FixKeyEventActionDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindExpertModeSetupDelegate(
        impl: ExpertModeSetupDelegateImpl,
    ): SystemBridgeSetupDelegate
}
