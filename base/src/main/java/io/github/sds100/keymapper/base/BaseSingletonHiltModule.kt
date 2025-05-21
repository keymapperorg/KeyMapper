package io.github.sds100.keymapper.base

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.base.actions.uielement.InteractUiElementController
import io.github.sds100.keymapper.base.actions.uielement.InteractUiElementUseCase
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCaseImpl
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCaseImpl
import io.github.sds100.keymapper.base.reroutekeyevents.RerouteKeyEventsUseCase
import io.github.sds100.keymapper.base.reroutekeyevents.RerouteKeyEventsUseCaseImpl
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityServiceAdapterImpl
import io.github.sds100.keymapper.base.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.base.system.accessibility.ControlAccessibilityServiceUseCaseImpl
import io.github.sds100.keymapper.base.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.base.system.inputmethod.ShowInputMethodPickerUseCaseImpl
import io.github.sds100.keymapper.base.system.inputmethod.ToggleCompatibleImeUseCase
import io.github.sds100.keymapper.base.system.inputmethod.ToggleCompatibleImeUseCaseImpl
import io.github.sds100.keymapper.base.system.notifications.AndroidNotificationAdapter
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProviderImpl
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
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
    abstract fun bindRerouteKeyEventsUseCase(impl: RerouteKeyEventsUseCaseImpl): RerouteKeyEventsUseCase
}
