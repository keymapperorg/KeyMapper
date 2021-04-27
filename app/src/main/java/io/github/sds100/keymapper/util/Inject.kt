package io.github.sds100.keymapper.util

import android.content.Context
import androidx.lifecycle.lifecycleScope
import io.github.sds100.keymapper.KeyMapperApp
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.UseCases
import io.github.sds100.keymapper.actions.*
import io.github.sds100.keymapper.backup.BackupRestoreMappingsUseCaseImpl
import io.github.sds100.keymapper.constraints.ChooseConstraintViewModel
import io.github.sds100.keymapper.home.HomeViewModel
import io.github.sds100.keymapper.home.ShowHomeScreenAlertsUseCaseImpl
import io.github.sds100.keymapper.mappings.fingerprintmaps.*
import io.github.sds100.keymapper.mappings.keymaps.*
import io.github.sds100.keymapper.onboarding.AppIntroSlide
import io.github.sds100.keymapper.onboarding.AppIntroUseCaseImpl
import io.github.sds100.keymapper.onboarding.AppIntroViewModel
import io.github.sds100.keymapper.settings.ConfigSettingsUseCaseImpl
import io.github.sds100.keymapper.settings.SettingsViewModel
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceController
import io.github.sds100.keymapper.system.accessibility.MyAccessibilityService
import io.github.sds100.keymapper.system.apps.ChooseActivityViewModel
import io.github.sds100.keymapper.system.apps.ChooseAppShortcutViewModel
import io.github.sds100.keymapper.system.apps.ChooseAppViewModel
import io.github.sds100.keymapper.system.apps.DisplayAppShortcutsUseCaseImpl
import io.github.sds100.keymapper.system.bluetooth.ChooseBluetoothDeviceUseCaseImpl
import io.github.sds100.keymapper.system.bluetooth.ChooseBluetoothDeviceViewModel
import io.github.sds100.keymapper.system.files.OnlineFileViewModel
import io.github.sds100.keymapper.system.intents.ConfigIntentViewModel
import io.github.sds100.keymapper.system.keyevents.ChooseKeyCodeViewModel
import io.github.sds100.keymapper.system.keyevents.ChooseKeyViewModel
import io.github.sds100.keymapper.system.keyevents.ConfigKeyEventViewModel
import io.github.sds100.keymapper.system.url.ChooseUrlViewModel
import io.github.sds100.keymapper.util.ui.TextBlockActionTypeViewModel

/**
 * Created by sds100 on 26/01/2020.
 */

object Inject {

    fun chooseAppViewModel(context: Context): ChooseAppViewModel.Factory {
        return ChooseAppViewModel.Factory(
            UseCases.displayPackages(context)
        )
    }

    fun chooseActivityViewModel(context: Context): ChooseActivityViewModel.Factory {
        return ChooseActivityViewModel.Factory(
            UseCases.displayPackages(context)
        )
    }

    fun chooseAppShortcutViewModel(context: Context): ChooseAppShortcutViewModel.Factory {
        return ChooseAppShortcutViewModel.Factory(
            DisplayAppShortcutsUseCaseImpl(
                ServiceLocator.appShortcutAdapter(context)
            ),
            ServiceLocator.resourceProvider(context)
        )
    }

    fun chooseConstraintListViewModel(ctx: Context): ChooseConstraintViewModel.Factory {
        return ChooseConstraintViewModel.Factory(ServiceLocator.resourceProvider(ctx))
    }

    fun keyActionTypeViewModel(): ChooseKeyViewModel.Factory {
        return ChooseKeyViewModel.Factory()
    }

    fun configKeyEventViewModel(
        context: Context
    ): ConfigKeyEventViewModel.Factory {
        return ConfigKeyEventViewModel.Factory(
            UseCases.getInputDevices(context),
            ServiceLocator.resourceProvider(context)
        )
    }

    fun chooseKeyCodeViewModel(): ChooseKeyCodeViewModel.Factory {
        return ChooseKeyCodeViewModel.Factory()
    }

    fun configIntentViewModel(ctx: Context): ConfigIntentViewModel.Factory {
        return ConfigIntentViewModel.Factory(ServiceLocator.resourceProvider(ctx))
    }

    fun textBlockActionTypeViewModel(): TextBlockActionTypeViewModel.Factory {
        return TextBlockActionTypeViewModel.Factory()
    }

    fun urlActionTypeViewModel(): ChooseUrlViewModel.Factory {
        return ChooseUrlViewModel.Factory()
    }

    fun tapCoordinateActionTypeViewModel(context: Context): PickDisplayCoordinateViewModel.Factory {
        return PickDisplayCoordinateViewModel.Factory(
            ServiceLocator.resourceProvider(context)
        )
    }

    fun systemActionListViewModel(ctx: Context): SystemActionListViewModel.Factory {
        return SystemActionListViewModel.Factory(
            CreateSystemActionUseCaseImpl(
                ServiceLocator.systemFeatureAdapter(ctx),
                ServiceLocator.packageManagerAdapter(ctx),
                ServiceLocator.inputMethodAdapter(ctx)
            ),

            ServiceLocator.resourceProvider(ctx)
        )
    }

    fun unsupportedActionListViewModel(
        context: Context
    ): UnsupportedActionListViewModel.Factory {
        return UnsupportedActionListViewModel.Factory(
            UseCases.isSystemActionSupported(context),
            ServiceLocator.resourceProvider(context)
        )
    }

    fun onlineFileViewModel(
        context: Context,
        fileUrl: String,
        alternateUrl: String? = null,
        header: String
    ): OnlineFileViewModel.Factory {
        return OnlineFileViewModel.Factory(
            ServiceLocator.fileRepository(context),
            fileUrl,
            alternateUrl,
            header
        )
    }

    fun configKeyMapViewModel(
        ctx: Context
    ): ConfigKeyMapViewModel.Factory {
        return ConfigKeyMapViewModel.Factory(
            UseCases.configKeyMap(ctx),
            TestActionUseCaseImpl(ServiceLocator.serviceAdapter(ctx)),
            UseCases.onboarding(ctx),
            (ctx.applicationContext as KeyMapperApp).recordTriggerController,
            UseCases.createKeymapShortcut(ctx),
            UseCases.displayKeyMap(ctx),
            ServiceLocator.resourceProvider(ctx)
        )
    }

    fun configFingerprintMapViewModel(
        ctx: Context
    ): ConfigFingerprintMapViewModel.Factory {
        return ConfigFingerprintMapViewModel.Factory(
            ConfigFingerprintMapUseCaseImpl(ServiceLocator.fingerprintMapRepository(ctx)),
            TestActionUseCaseImpl(ServiceLocator.serviceAdapter(ctx)),
            UseCases.displaySimpleMapping(ctx),
            ServiceLocator.resourceProvider(ctx)
        )
    }

    fun createActionShortcutViewModel(
        ctx: Context
    ): CreateKeyMapShortcutViewModel.Factory {
        return CreateKeyMapShortcutViewModel.Factory(
            UseCases.configKeyMap(ctx),
            ListKeyMapsUseCaseImpl(
                ServiceLocator.roomKeymapRepository(ctx),
                ServiceLocator.backupManager(ctx),
                UseCases.displayKeyMap(ctx)
            ),
            UseCases.createKeymapShortcut(ctx),
            ServiceLocator.resourceProvider(ctx)
        )
    }

    fun homeViewModel(ctx: Context): HomeViewModel.Factory {
        return HomeViewModel.Factory(
            ListKeyMapsUseCaseImpl(
                ServiceLocator.roomKeymapRepository(ctx),
                ServiceLocator.backupManager(ctx),
                UseCases.displayKeyMap(ctx)
            ),
            ListFingerprintMapsUseCaseImpl(
                ServiceLocator.fingerprintMapRepository(ctx),
                ServiceLocator.backupManager(ctx),
                ServiceLocator.preferenceRepository(ctx),
                UseCases.displaySimpleMapping(ctx)
            ),
            UseCases.pauseMappings(ctx),
            BackupRestoreMappingsUseCaseImpl(ServiceLocator.backupManager(ctx)),
            ShowHomeScreenAlertsUseCaseImpl(
                ServiceLocator.preferenceRepository(ctx),
                ServiceLocator.permissionAdapter(ctx),
                UseCases.controlAccessibilityService(ctx)
            ),
            UseCases.showImePicker(ctx),
            UseCases.onboarding(ctx),
            ServiceLocator.resourceProvider(ctx)
        )
    }

    fun settingsViewModel(context: Context): SettingsViewModel.Factory {
        return SettingsViewModel.Factory(
            ConfigSettingsUseCaseImpl(
                ServiceLocator.preferenceRepository(context),
                ServiceLocator.permissionAdapter(context),
                ServiceLocator.inputMethodAdapter(context),
                ServiceLocator.suAdapter(context),
            ),
            ServiceLocator.resourceProvider(context)
        )
    }

    fun appIntroViewModel(
        context: Context,
        slides: List<AppIntroSlide>
    ): AppIntroViewModel.Factory {
        return AppIntroViewModel.Factory(
            AppIntroUseCaseImpl(
                ServiceLocator.permissionAdapter(context),
                ServiceLocator.serviceAdapter(context),
                ServiceLocator.systemFeatureAdapter(context),
                ServiceLocator.preferenceRepository(context),
                UseCases.fingerprintGesturesSupported(context)
            ),
            slides,
            ServiceLocator.resourceProvider(context)
        )
    }

    fun accessibilityServiceController(service: MyAccessibilityService): AccessibilityServiceController {
        return AccessibilityServiceController(
            coroutineScope = service.lifecycleScope,
            accessibilityService = service,
            inputEvents = ServiceLocator.serviceAdapter(service).serviceOutputEvents,
            outputEvents = ServiceLocator.serviceAdapter(service).eventReceiver,
            detectConstraintsUseCase = UseCases.detectConstraints(service),
            performActionsUseCase = UseCases.performActions(service, service),
            detectKeyMapsUseCase = UseCases.detectKeyMaps(service),
            detectFingerprintMapsUseCase = UseCases.detectFingerprintMaps(service),
            pauseMappingsUseCase = UseCases.pauseMappings(service),
            devicesAdapter = ServiceLocator.devicesAdapter(service),
            suAdapter = ServiceLocator.suAdapter(service),
            rerouteKeyEventsUseCase = UseCases.rerouteKeyEvents(service)
        )
    }

    fun chooseBluetoothDeviceViewModel(ctx: Context): ChooseBluetoothDeviceViewModel.Factory {
        return ChooseBluetoothDeviceViewModel.Factory(
            ChooseBluetoothDeviceUseCaseImpl(ServiceLocator.devicesAdapter(ctx)),
            ServiceLocator.resourceProvider(ctx)
        )
    }
}