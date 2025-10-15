package io.github.sds100.keymapper.base

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.base.actions.GetActionErrorUseCase
import io.github.sds100.keymapper.base.actions.GetActionErrorUseCaseImpl
import io.github.sds100.keymapper.base.actions.sound.SoundsManager
import io.github.sds100.keymapper.base.actions.sound.SoundsManagerImpl
import io.github.sds100.keymapper.base.actions.uielement.InteractUiElementController
import io.github.sds100.keymapper.base.actions.uielement.InteractUiElementUseCase
import io.github.sds100.keymapper.base.backup.BackupManager
import io.github.sds100.keymapper.base.backup.BackupManagerImpl
import io.github.sds100.keymapper.base.constraints.GetConstraintErrorUseCase
import io.github.sds100.keymapper.base.constraints.GetConstraintErrorUseCaseImpl
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.input.InputEventHubImpl
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapState
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapStateImpl
import io.github.sds100.keymapper.base.keymaps.EnableKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.EnableKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCaseImpl
import io.github.sds100.keymapper.base.keymaps.GetDefaultKeyMapOptionsUseCase
import io.github.sds100.keymapper.base.keymaps.GetDefaultKeyMapOptionsUseCaseImpl
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCaseImpl
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityServiceAdapterImpl
import io.github.sds100.keymapper.base.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.base.system.accessibility.ControlAccessibilityServiceUseCaseImpl
import io.github.sds100.keymapper.base.system.inputmethod.ImeInputEventInjector
import io.github.sds100.keymapper.base.system.inputmethod.ImeInputEventInjectorImpl
import io.github.sds100.keymapper.base.system.inputmethod.ShowHideInputMethodUseCase
import io.github.sds100.keymapper.base.system.inputmethod.ShowHideInputMethodUseCaseImpl
import io.github.sds100.keymapper.base.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.base.system.inputmethod.ShowInputMethodPickerUseCaseImpl
import io.github.sds100.keymapper.base.system.inputmethod.SwitchImeAsyncImpl
import io.github.sds100.keymapper.base.system.inputmethod.SwitchImeInterface
import io.github.sds100.keymapper.base.system.inputmethod.ToggleCompatibleImeUseCase
import io.github.sds100.keymapper.base.system.inputmethod.ToggleCompatibleImeUseCaseImpl
import io.github.sds100.keymapper.base.system.notifications.AndroidNotificationAdapter
import io.github.sds100.keymapper.base.system.notifications.ManageNotificationsUseCase
import io.github.sds100.keymapper.base.system.notifications.ManageNotificationsUseCaseImpl
import io.github.sds100.keymapper.base.trigger.RecordTriggerController
import io.github.sds100.keymapper.base.trigger.RecordTriggerControllerImpl
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.NavigationProviderImpl
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProviderImpl
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProviderImpl
import io.github.sds100.keymapper.common.utils.DefaultUuidGenerator
import io.github.sds100.keymapper.common.utils.UuidGenerator
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyEventRelayServiceWrapper
import io.github.sds100.keymapper.system.inputmethod.KeyEventRelayServiceWrapperImpl
import io.github.sds100.keymapper.system.notifications.NotificationAdapter
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BaseSingletonHiltModule {
    @Singleton
    @Binds
    abstract fun provideNotificationAdapter(impl: AndroidNotificationAdapter): NotificationAdapter

    @Singleton
    @Binds
    abstract fun provideAccessibilityAdapter(impl: AccessibilityServiceAdapterImpl): AccessibilityServiceAdapter

    @Singleton
    @Binds
    abstract fun provideResourceProvider(impl: ResourceProviderImpl): ResourceProvider

    @Singleton
    @Binds
    abstract fun provideOnboardingUseCase(impl: OnboardingUseCaseImpl): OnboardingUseCase

    @Binds
    @Singleton
    abstract fun bindPauseKeyMapsUseCase(impl: PauseKeyMapsUseCaseImpl): PauseKeyMapsUseCase

    @Binds
    @Singleton
    abstract fun bindShowInputMethodPickerUseCase(impl: ShowInputMethodPickerUseCaseImpl): ShowInputMethodPickerUseCase

    @Binds
    @Singleton
    abstract fun bindControlAccessibilityServiceUseCase(impl: ControlAccessibilityServiceUseCaseImpl): ControlAccessibilityServiceUseCase

    @Binds
    @Singleton
    abstract fun bindToggleCompatibleImeUseCase(impl: ToggleCompatibleImeUseCaseImpl): ToggleCompatibleImeUseCase

    @Binds
    @Singleton
    abstract fun bindInteractUiElementUseCase(impl: InteractUiElementController): InteractUiElementUseCase

    @Binds
    @Singleton
    abstract fun bindShowHideInputMethodUseCase(impl: ShowHideInputMethodUseCaseImpl): ShowHideInputMethodUseCase

    @Binds
    @Singleton
    abstract fun bindBackupManager(impl: BackupManagerImpl): BackupManager

    @Binds
    @Singleton
    abstract fun bindSoundsManager(impl: SoundsManagerImpl): SoundsManager

    @Binds
    @Singleton
    abstract fun bindRecordTriggerUseCase(impl: RecordTriggerControllerImpl): RecordTriggerController

    @Binds
    @Singleton
    abstract fun bindFingerprintGesturesSupportedUseCase(impl: FingerprintGesturesSupportedUseCaseImpl): FingerprintGesturesSupportedUseCase

    @Binds
    @Singleton
    abstract fun bindGetActionErrorUseCase(impl: GetActionErrorUseCaseImpl): GetActionErrorUseCase

    @Binds
    @Singleton
    abstract fun bindGetConstraintErrorUseCase(impl: GetConstraintErrorUseCaseImpl): GetConstraintErrorUseCase

    @Binds
    @Singleton
    abstract fun bindManageNotificationsUseCase(impl: ManageNotificationsUseCaseImpl): ManageNotificationsUseCase

    @Binds
    @Singleton
    abstract fun bindUuidGenerator(impl: DefaultUuidGenerator): UuidGenerator

    @Binds
    @Singleton
    abstract fun bindNavigationProvider(impl: NavigationProviderImpl): NavigationProvider

    @Binds
    @Singleton
    abstract fun bindDialogProvider(impl: DialogProviderImpl): DialogProvider

    @Binds
    @Singleton
    abstract fun bindInputEventHub(impl: InputEventHubImpl): InputEventHub

    @Binds
    @Singleton
    abstract fun keyEventRelayServiceWrapper(impl: KeyEventRelayServiceWrapperImpl): KeyEventRelayServiceWrapper

    @Binds
    @Singleton
    abstract fun imeInputEvenInjector(impl: ImeInputEventInjectorImpl): ImeInputEventInjector

    @Binds
    @Singleton
    abstract fun bindConfigKeyMapState(impl: ConfigKeyMapStateImpl): ConfigKeyMapState

    @Binds
    @Singleton
    abstract fun bindGetDefaultKeyMapOptionsUseCas(impl: GetDefaultKeyMapOptionsUseCaseImpl): GetDefaultKeyMapOptionsUseCase

    @Binds
    @Singleton
    abstract fun bindSwitchImeInterface(impl: SwitchImeAsyncImpl): SwitchImeInterface

    @Binds
    @Singleton
    abstract fun bindEnableKeyMapsUseCase(impl: EnableKeyMapsUseCaseImpl): EnableKeyMapsUseCase
}
