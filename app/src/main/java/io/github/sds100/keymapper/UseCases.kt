package io.github.sds100.keymapper

import android.content.Context
import io.github.sds100.keymapper.actions.CreateActionUseCaseImpl
import io.github.sds100.keymapper.actions.GetActionErrorUseCaseImpl
import io.github.sds100.keymapper.actions.IsActionSupportedUseCaseImpl
import io.github.sds100.keymapper.actions.PerformActionsUseCaseImpl
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCaseImpl
import io.github.sds100.keymapper.constraints.GetConstraintErrorUseCaseImpl
import io.github.sds100.keymapper.mappings.DisplaySimpleMappingUseCase
import io.github.sds100.keymapper.mappings.DisplaySimpleMappingUseCaseImpl
import io.github.sds100.keymapper.mappings.PauseMappingsUseCaseImpl
import io.github.sds100.keymapper.mappings.fingerprintmaps.AreFingerprintGesturesSupportedUseCaseImpl
import io.github.sds100.keymapper.mappings.fingerprintmaps.DetectFingerprintMapsUseCaseImpl
import io.github.sds100.keymapper.mappings.keymaps.*
import io.github.sds100.keymapper.mappings.keymaps.detection.DetectKeyMapsUseCaseImpl
import io.github.sds100.keymapper.onboarding.OnboardingUseCaseImpl
import io.github.sds100.keymapper.reroutekeyevents.RerouteKeyEventsUseCaseImpl
import io.github.sds100.keymapper.shizuku.ShizukuInputEventInjector
import io.github.sds100.keymapper.system.Shell
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCaseImpl
import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.accessibility.MyAccessibilityService
import io.github.sds100.keymapper.system.apps.DisplayAppsUseCase
import io.github.sds100.keymapper.system.apps.DisplayAppsUseCaseImpl
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeMessengerImpl
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCaseImpl
import io.github.sds100.keymapper.system.inputmethod.ToggleCompatibleImeUseCaseImpl

/**
 * Created by sds100 on 03/03/2021.
 */
object UseCases {

    fun displayPackages(ctx: Context): DisplayAppsUseCase =
        DisplayAppsUseCaseImpl(
            ServiceLocator.packageManagerAdapter(ctx)
        )

    fun displayKeyMap(ctx: Context): DisplayKeyMapUseCase {
        return DisplayKeyMapUseCaseImpl(
            ServiceLocator.permissionAdapter(ctx),
            ServiceLocator.inputMethodAdapter(ctx),
            displaySimpleMapping(ctx)
        )
    }

    fun configKeyMap(ctx: Context): ConfigKeyMapUseCase {
        return ConfigKeyMapUseCaseImpl(
            ServiceLocator.roomKeymapRepository(ctx),
            ServiceLocator.devicesAdapter(ctx),
            ServiceLocator.settingsRepository(ctx)
        )
    }

    fun displaySimpleMapping(ctx: Context): DisplaySimpleMappingUseCase {
        return DisplaySimpleMappingUseCaseImpl(
            ServiceLocator.packageManagerAdapter(ctx),
            ServiceLocator.permissionAdapter(ctx),
            ServiceLocator.inputMethodAdapter(ctx),
            ServiceLocator.settingsRepository(ctx),
            ServiceLocator.accessibilityServiceAdapter(ctx),
            getActionError(ctx),
            getConstraintError(ctx)
        )
    }

    fun getActionError(ctx: Context) = GetActionErrorUseCaseImpl(
        ServiceLocator.packageManagerAdapter(ctx),
        ServiceLocator.inputMethodAdapter(ctx),
        ServiceLocator.permissionAdapter(ctx),
        ServiceLocator.systemFeatureAdapter(ctx),
        ServiceLocator.cameraAdapter(ctx),
        ServiceLocator.soundsManager(ctx),
        ServiceLocator.shizukuAdapter(ctx)
    )

    fun getConstraintError(ctx: Context) = GetConstraintErrorUseCaseImpl(
        ServiceLocator.packageManagerAdapter(ctx),
        ServiceLocator.permissionAdapter(ctx),
        ServiceLocator.systemFeatureAdapter(ctx),
        ServiceLocator.inputMethodAdapter(ctx)
    )

    fun onboarding(ctx: Context) = OnboardingUseCaseImpl(
        ServiceLocator.settingsRepository(ctx),
        ServiceLocator.fileAdapter(ctx),
        ServiceLocator.leanbackAdapter(ctx),
        ServiceLocator.shizukuAdapter(ctx),
        ServiceLocator.permissionAdapter(ctx),
        ServiceLocator.packageManagerAdapter(ctx)
    )

    fun createKeymapShortcut(ctx: Context) = CreateKeyMapShortcutUseCaseImpl(
        ServiceLocator.appShortcutAdapter(ctx),
        displayKeyMap(ctx),
        ServiceLocator.resourceProvider(ctx)
    )

    fun isActionSupported(ctx: Context) =
        IsActionSupportedUseCaseImpl(ServiceLocator.systemFeatureAdapter(ctx))

    fun fingerprintGesturesSupported(ctx: Context) =
        AreFingerprintGesturesSupportedUseCaseImpl(ServiceLocator.settingsRepository(ctx))

    fun pauseMappings(ctx: Context) =
        PauseMappingsUseCaseImpl(
            ServiceLocator.settingsRepository(ctx),
            ServiceLocator.mediaAdapter(ctx)
        )

    fun showImePicker(ctx: Context): ShowInputMethodPickerUseCase {
        return ShowInputMethodPickerUseCaseImpl(
            ServiceLocator.inputMethodAdapter(ctx)
        )
    }

    fun controlAccessibilityService(ctx: Context): ControlAccessibilityServiceUseCase {
        return ControlAccessibilityServiceUseCaseImpl(
            ServiceLocator.accessibilityServiceAdapter(ctx)
        )
    }

    fun toggleCompatibleIme(ctx: Context) =
        ToggleCompatibleImeUseCaseImpl(
            ServiceLocator.inputMethodAdapter(ctx)
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
        ServiceLocator.powerAdapter(service)
    )

    fun performActions(ctx: Context, service: IAccessibilityService) =
        PerformActionsUseCaseImpl(
            (ctx.applicationContext as KeyMapperApp).appCoroutineScope,
            service,
            ServiceLocator.inputMethodAdapter(ctx),
            ServiceLocator.fileAdapter(ctx),
            ServiceLocator.suAdapter(ctx),
            Shell,
            ServiceLocator.intentAdapter(ctx),
            getActionError(ctx),
            keyMapperImeMessenger(ctx),
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
            ServiceLocator.notificationReceiverAdapter(ctx)
        )

    fun detectKeyMaps(service: MyAccessibilityService) = DetectKeyMapsUseCaseImpl(
        ServiceLocator.roomKeymapRepository(service),
        ServiceLocator.settingsRepository(service),
        ServiceLocator.suAdapter(service),
        ServiceLocator.displayAdapter(service),
        ServiceLocator.audioAdapter(service),
        keyMapperImeMessenger(service),
        service,
        ShizukuInputEventInjector(),
        ServiceLocator.permissionAdapter(service),
        ServiceLocator.phoneAdapter(service),
        ServiceLocator.inputMethodAdapter(service),
        ServiceLocator.vibratorAdapter(service),
        ServiceLocator.popupMessageAdapter(service),
        ServiceLocator.resourceProvider(service)
    )

    fun detectFingerprintMaps(ctx: Context) = DetectFingerprintMapsUseCaseImpl(
        ServiceLocator.fingerprintMapRepository(ctx),
        fingerprintGesturesSupported(ctx),
        ServiceLocator.vibratorAdapter(ctx),
        ServiceLocator.settingsRepository(ctx),
        ServiceLocator.popupMessageAdapter(ctx),
        ServiceLocator.resourceProvider(ctx)
    )

    fun rerouteKeyEvents(ctx: Context) = RerouteKeyEventsUseCaseImpl(
        ServiceLocator.inputMethodAdapter(ctx),
        keyMapperImeMessenger(ctx),
        ServiceLocator.settingsRepository(ctx)
    )

    fun createAction(ctx: Context) = CreateActionUseCaseImpl(
        ServiceLocator.inputMethodAdapter(ctx)
    )

    private fun keyMapperImeMessenger(ctx: Context) = KeyMapperImeMessengerImpl(
        ctx,
        ServiceLocator.inputMethodAdapter(ctx)
    )
}