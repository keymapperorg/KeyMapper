package io.github.sds100.keymapper

import android.content.Context
import io.github.sds100.keymapper.actions.CreateActionUseCaseImpl
import io.github.sds100.keymapper.actions.GetActionErrorUseCaseImpl
import io.github.sds100.keymapper.actions.PerformActionsUseCaseImpl
import io.github.sds100.keymapper.api.KeyEventRelayServiceWrapper
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCaseImpl
import io.github.sds100.keymapper.constraints.GetConstraintErrorUseCaseImpl
import io.github.sds100.keymapper.floating.ListFloatingLayoutsUseCase
import io.github.sds100.keymapper.floating.ListFloatingLayoutsUseCaseImpl
import io.github.sds100.keymapper.mappings.FingerprintGesturesSupportedUseCaseImpl
import io.github.sds100.keymapper.mappings.PauseKeyMapsUseCaseImpl
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.CreateKeyMapShortcutUseCaseImpl
import io.github.sds100.keymapper.mappings.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.DisplayKeyMapUseCaseImpl
import io.github.sds100.keymapper.mappings.keymaps.detection.DetectKeyMapsUseCaseImpl
import io.github.sds100.keymapper.onboarding.OnboardingUseCaseImpl
import io.github.sds100.keymapper.reroutekeyevents.RerouteKeyEventsUseCaseImpl
import io.github.sds100.keymapper.shizuku.ShizukuInputEventInjector
import io.github.sds100.keymapper.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.sorting.SortKeyMapsUseCaseImpl
import io.github.sds100.keymapper.system.Shell
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCaseImpl
import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.accessibility.MyAccessibilityService
import io.github.sds100.keymapper.system.apps.DisplayAppsUseCase
import io.github.sds100.keymapper.system.apps.DisplayAppsUseCaseImpl
import io.github.sds100.keymapper.system.inputmethod.ImeInputEventInjectorImpl
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCaseImpl
import io.github.sds100.keymapper.system.inputmethod.ToggleCompatibleImeUseCaseImpl

/**
 * Created by sds100 on 03/03/2021.
 */
object UseCases {

    fun listFloatingLayouts(ctx: Context): ListFloatingLayoutsUseCase = ListFloatingLayoutsUseCaseImpl(
        ServiceLocator.floatingLayoutRepository(ctx),
        ServiceLocator.purchasingManager(ctx),
        ServiceLocator.accessibilityServiceAdapter(ctx),
        ServiceLocator.settingsRepository(ctx),
    )

    fun displayPackages(ctx: Context): DisplayAppsUseCase = DisplayAppsUseCaseImpl(
        ServiceLocator.packageManagerAdapter(ctx),
    )

    fun displayKeyMap(ctx: Context): DisplayKeyMapUseCase = DisplayKeyMapUseCaseImpl(
        ServiceLocator.permissionAdapter(ctx),
        ServiceLocator.inputMethodAdapter(ctx),
        ServiceLocator.packageManagerAdapter(ctx),
        ServiceLocator.settingsRepository(ctx),
        ServiceLocator.accessibilityServiceAdapter(ctx),
        ServiceLocator.settingsRepository(ctx),
        ServiceLocator.purchasingManager(ctx),
        getActionError(ctx),
        getConstraintError(ctx),
    )

    fun configKeyMap(ctx: Context): ConfigKeyMapUseCase = ServiceLocator.configKeyMapsController(ctx)

    fun getActionError(ctx: Context) = GetActionErrorUseCaseImpl(
        ServiceLocator.packageManagerAdapter(ctx),
        ServiceLocator.inputMethodAdapter(ctx),
        ServiceLocator.permissionAdapter(ctx),
        ServiceLocator.systemFeatureAdapter(ctx),
        ServiceLocator.cameraAdapter(ctx),
        ServiceLocator.soundsManager(ctx),
        ServiceLocator.shizukuAdapter(ctx),
    )

    fun getConstraintError(ctx: Context) = GetConstraintErrorUseCaseImpl(
        ServiceLocator.packageManagerAdapter(ctx),
        ServiceLocator.permissionAdapter(ctx),
        ServiceLocator.systemFeatureAdapter(ctx),
        ServiceLocator.inputMethodAdapter(ctx),
        ServiceLocator.cameraAdapter(ctx),
    )

    fun onboarding(ctx: Context) = OnboardingUseCaseImpl(
        ServiceLocator.settingsRepository(ctx),
        ServiceLocator.fileAdapter(ctx),
        ServiceLocator.leanbackAdapter(ctx),
        ServiceLocator.shizukuAdapter(ctx),
        ServiceLocator.permissionAdapter(ctx),
        ServiceLocator.packageManagerAdapter(ctx),
    )

    fun createKeymapShortcut(ctx: Context) = CreateKeyMapShortcutUseCaseImpl(
        ServiceLocator.appShortcutAdapter(ctx),
        ServiceLocator.resourceProvider(ctx),
    )

    fun fingerprintGesturesSupported(ctx: Context) = FingerprintGesturesSupportedUseCaseImpl(ServiceLocator.settingsRepository(ctx))

    fun pauseMappings(ctx: Context) = PauseKeyMapsUseCaseImpl(
        ServiceLocator.settingsRepository(ctx),
        ServiceLocator.mediaAdapter(ctx),
    )

    fun showImePicker(ctx: Context): ShowInputMethodPickerUseCase = ShowInputMethodPickerUseCaseImpl(
        ServiceLocator.inputMethodAdapter(ctx),
    )

    fun controlAccessibilityService(ctx: Context): ControlAccessibilityServiceUseCase = ControlAccessibilityServiceUseCaseImpl(
        ServiceLocator.accessibilityServiceAdapter(ctx),
        ServiceLocator.permissionAdapter(ctx),
    )

    fun toggleCompatibleIme(ctx: Context) = ToggleCompatibleImeUseCaseImpl(
        ServiceLocator.inputMethodAdapter(ctx),
    )

    fun detectConstraints(service: MyAccessibilityService) = DetectConstraintsUseCaseImpl(
        service,
        ServiceLocator.mediaAdapter(service),
        ServiceLocator.devicesAdapter(service),
        ServiceLocator.displayAdapter(service),
        ServiceLocator.cameraAdapter(service),
        ServiceLocator.networkAdapter(service),
        ServiceLocator.inputMethodAdapter(service),
        ServiceLocator.lockScreenAdapter(service),
        ServiceLocator.phoneAdapter(service),
        ServiceLocator.powerAdapter(service),
    )

    fun performActions(
        ctx: Context,
        service: IAccessibilityService,
        keyEventRelayService: KeyEventRelayServiceWrapper,
    ) = PerformActionsUseCaseImpl(
        (ctx.applicationContext as KeyMapperApp).appCoroutineScope,
        service,
        ServiceLocator.inputMethodAdapter(ctx),
        ServiceLocator.fileAdapter(ctx),
        ServiceLocator.suAdapter(ctx),
        Shell,
        ServiceLocator.intentAdapter(ctx),
        getActionError(ctx),
        keyMapperImeMessenger(ctx, keyEventRelayService),
        ShizukuInputEventInjector(),
        ServiceLocator.packageManagerAdapter(ctx),
        ServiceLocator.appShortcutAdapter(ctx),
        ServiceLocator.popupMessageAdapter(ctx),
        ServiceLocator.devicesAdapter(ctx),
        ServiceLocator.phoneAdapter(ctx),
        ServiceLocator.audioAdapter(ctx),
        ServiceLocator.cameraAdapter(ctx),
        ServiceLocator.displayAdapter(ctx),
        ServiceLocator.lockScreenAdapter(ctx),
        ServiceLocator.mediaAdapter(ctx),
        ServiceLocator.airplaneModeAdapter(ctx),
        ServiceLocator.networkAdapter(ctx),
        ServiceLocator.bluetoothAdapter(ctx),
        ServiceLocator.nfcAdapter(ctx),
        ServiceLocator.openUrlAdapter(ctx),
        ServiceLocator.resourceProvider(ctx),
        ServiceLocator.settingsRepository(ctx),
        ServiceLocator.soundsManager(ctx),
        ServiceLocator.permissionAdapter(ctx),
        ServiceLocator.notificationReceiverAdapter(ctx),
    )

    fun detectKeyMaps(
        ctx: Context,
        service: IAccessibilityService,
        keyEventRelayService: KeyEventRelayServiceWrapper,
    ) = DetectKeyMapsUseCaseImpl(
        ServiceLocator.roomKeyMapRepository(ctx),
        ServiceLocator.floatingButtonRepository(ctx),
        ServiceLocator.settingsRepository(ctx),
        ServiceLocator.suAdapter(ctx),
        ServiceLocator.displayAdapter(ctx),
        ServiceLocator.audioAdapter(ctx),
        keyMapperImeMessenger(ctx, keyEventRelayService),
        service,
        ShizukuInputEventInjector(),
        ServiceLocator.popupMessageAdapter(ctx),
        ServiceLocator.permissionAdapter(ctx),
        ServiceLocator.resourceProvider(ctx),
        ServiceLocator.vibratorAdapter(ctx),
    )

    fun rerouteKeyEvents(ctx: Context, keyEventRelayService: KeyEventRelayServiceWrapper) = RerouteKeyEventsUseCaseImpl(
        ServiceLocator.inputMethodAdapter(ctx),
        keyMapperImeMessenger(ctx, keyEventRelayService),
        ServiceLocator.settingsRepository(ctx),
    )

    fun createAction(ctx: Context) = CreateActionUseCaseImpl(
        ServiceLocator.inputMethodAdapter(ctx),
        ServiceLocator.systemFeatureAdapter(ctx),
        ServiceLocator.cameraAdapter(ctx),
        ServiceLocator.permissionAdapter(ctx),
    )

    private fun keyMapperImeMessenger(
        ctx: Context,
        keyEventRelayService: KeyEventRelayServiceWrapper,
    ) = ImeInputEventInjectorImpl(
        ctx,
        keyEventRelayService,
        ServiceLocator.inputMethodAdapter(ctx),
    )

    fun sortKeyMapsUseCase(ctx: Context): SortKeyMapsUseCase = SortKeyMapsUseCaseImpl(
        ServiceLocator.settingsRepository(ctx),
        displayKeyMap(ctx),
    )
}
