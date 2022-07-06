package io.github.sds100.keymapper.system

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.shizuku.ShizukuAdapter
import io.github.sds100.keymapper.shizuku.ShizukuAdapterImpl
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCaseImpl
import io.github.sds100.keymapper.system.inputmethod.*
import io.github.sds100.keymapper.system.media.AndroidMediaAdapter
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.system.notifications.AndroidNotificationAdapter
import io.github.sds100.keymapper.system.notifications.ManageNotificationsUseCase
import io.github.sds100.keymapper.system.notifications.ManageNotificationsUseCaseImpl
import io.github.sds100.keymapper.system.notifications.NotificationAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.root.SuAdapterImpl
import javax.inject.Singleton

/**
 * Created by sds100 on 28/06/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SystemModule {

    @Binds
    abstract fun bindManageNotificationsUseCase(impl: ManageNotificationsUseCaseImpl): ManageNotificationsUseCase

    @Binds
    abstract fun bindShowInputMethodPickerUseCase(imple: ShowInputMethodPickerUseCaseImpl): ShowInputMethodPickerUseCase

    @Binds
    abstract fun bindControlAccessibilityServiceUseCase(impl: ControlAccessibilityServiceUseCaseImpl): ControlAccessibilityServiceUseCase

    @Binds
    abstract fun bindToggleCompatibleImeUseCase(imple: ToggleCompatibleImeUseCaseImpl): ToggleCompatibleImeUseCase

    @Binds
    abstract fun bindShowHideInputMethodUseCase(imple: ShowHideInputMethodUseCaseImpl): ShowHideInputMethodUseCase

    @Binds
    @Singleton
    abstract fun bindNotificationAdapter(impl: AndroidNotificationAdapter): NotificationAdapter

    @Binds
    @Singleton
    abstract fun bindSuAdapter(impl: SuAdapterImpl): SuAdapter

    @Binds
    @Singleton
    abstract fun bindMediaAdapter(impl: AndroidMediaAdapter): MediaAdapter

    @Binds
    @Singleton
    abstract fun bindShizukuAdapter(impl: ShizukuAdapterImpl): ShizukuAdapter

    @Binds
    @Singleton
    abstract fun bindInputMethodAdapter(impl: AndroidInputMethodAdapter): InputMethodAdapter
    
//    @Binds
//    @Singleton
//    abstract fun bindNotificationController(): NotificationController
//
//    @Binds
//    @Singleton
//    abstract fun bindAutoSwitchImeController(): AutoSwitchImeController
//
//    @Binds
//    @Singleton
//    abstract fun bindAutoGrantPermissionController(): AutoGrantPermissionController
}