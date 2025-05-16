package io.github.sds100.keymapper.util

import android.content.Context
import androidx.lifecycle.lifecycleScope
import io.github.sds100.keymapper.KeyMapperApp
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.UseCases
import io.github.sds100.keymapper.actions.ChooseActionViewModel
import io.github.sds100.keymapper.actions.TestActionUseCaseImpl
import io.github.sds100.keymapper.actions.keyevent.ChooseKeyCodeViewModel
import io.github.sds100.keymapper.actions.keyevent.ConfigKeyEventActionViewModel
import io.github.sds100.keymapper.actions.keyevent.ConfigKeyEventUseCaseImpl
import io.github.sds100.keymapper.actions.pinchscreen.PinchPickDisplayCoordinateViewModel
import io.github.sds100.keymapper.actions.sound.ChooseSoundFileUseCaseImpl
import io.github.sds100.keymapper.actions.sound.ChooseSoundFileViewModel
import io.github.sds100.keymapper.actions.swipescreen.SwipePickDisplayCoordinateViewModel
import io.github.sds100.keymapper.actions.tapscreen.PickDisplayCoordinateViewModel
import io.github.sds100.keymapper.actions.uielement.InteractUiElementViewModel
import io.github.sds100.keymapper.api.KeyEventRelayServiceWrapper
import io.github.sds100.keymapper.backup.BackupRestoreMappingsUseCaseImpl
import io.github.sds100.keymapper.constraints.ChooseConstraintViewModel
import io.github.sds100.keymapper.constraints.CreateConstraintUseCaseImpl
import io.github.sds100.keymapper.home.HomeViewModel
import io.github.sds100.keymapper.home.ShowHomeScreenAlertsUseCaseImpl
import io.github.sds100.keymapper.keymaps.ConfigKeyMapViewModel
import io.github.sds100.keymapper.keymaps.CreateKeyMapShortcutViewModel
import io.github.sds100.keymapper.keymaps.FingerprintGesturesSupportedUseCaseImpl
import io.github.sds100.keymapper.keymaps.ListKeyMapsUseCaseImpl
import io.github.sds100.keymapper.logging.DisplayLogUseCaseImpl
import io.github.sds100.keymapper.logging.LogViewModel
import io.github.sds100.keymapper.settings.ConfigSettingsUseCaseImpl
import io.github.sds100.keymapper.settings.SettingsViewModel
import io.github.sds100.keymapper.sorting.SortKeyMapsUseCaseImpl
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceController
import io.github.sds100.keymapper.system.accessibility.MyAccessibilityService
import io.github.sds100.keymapper.system.apps.ChooseActivityViewModel
import io.github.sds100.keymapper.system.apps.ChooseAppShortcutViewModel
import io.github.sds100.keymapper.system.apps.ChooseAppViewModel
import io.github.sds100.keymapper.system.apps.DisplayAppShortcutsUseCaseImpl
import io.github.sds100.keymapper.system.bluetooth.ChooseBluetoothDeviceUseCaseImpl
import io.github.sds100.keymapper.system.bluetooth.ChooseBluetoothDeviceViewModel
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCaseImpl
import io.github.sds100.keymapper.system.intents.ConfigIntentViewModel
import io.github.sds100.keymapper.trigger.SetupGuiKeyboardUseCaseImpl

object Inject {

    fun chooseActionViewModel(ctx: Context): ChooseActionViewModel.Factory = ChooseActionViewModel.Factory(
        UseCases.createAction(ctx),
        ServiceLocator.resourceProvider(ctx),
    )

    fun chooseAppViewModel(context: Context): ChooseAppViewModel.Factory = ChooseAppViewModel.Factory(
        UseCases.displayPackages(context),
    )

    fun chooseActivityViewModel(context: Context): ChooseActivityViewModel.Factory = ChooseActivityViewModel.Factory(
        UseCases.displayPackages(context),
    )

    fun chooseAppShortcutViewModel(context: Context): ChooseAppShortcutViewModel.Factory = ChooseAppShortcutViewModel.Factory(
        DisplayAppShortcutsUseCaseImpl(
            ServiceLocator.appShortcutAdapter(context),
        ),
        ServiceLocator.resourceProvider(context),
    )

    fun chooseConstraintListViewModel(ctx: Context): ChooseConstraintViewModel.Factory = ChooseConstraintViewModel.Factory(
        CreateConstraintUseCaseImpl(
            ServiceLocator.networkAdapter(ctx),
            ServiceLocator.inputMethodAdapter(ctx),
            ServiceLocator.settingsRepository(ctx),
            ServiceLocator.cameraAdapter(ctx),
        ),
        ServiceLocator.resourceProvider(ctx),
    )

    fun configKeyEventViewModel(
        context: Context,
    ): ConfigKeyEventActionViewModel.Factory {
        val useCase = ConfigKeyEventUseCaseImpl(
            preferenceRepository = ServiceLocator.settingsRepository(context),
            devicesAdapter = ServiceLocator.devicesAdapter(context),
        )
        return ConfigKeyEventActionViewModel.Factory(
            useCase,
            ServiceLocator.resourceProvider(context),
        )
    }

    fun chooseKeyCodeViewModel(): ChooseKeyCodeViewModel.Factory = ChooseKeyCodeViewModel.Factory()

    fun configIntentViewModel(ctx: Context): ConfigIntentViewModel.Factory = ConfigIntentViewModel.Factory(ServiceLocator.resourceProvider(ctx))

    fun soundFileActionTypeViewModel(ctx: Context): ChooseSoundFileViewModel.Factory = ChooseSoundFileViewModel.Factory(
        ServiceLocator.resourceProvider(ctx),
        ChooseSoundFileUseCaseImpl(
            ServiceLocator.fileAdapter(ctx),
            ServiceLocator.soundsManager(ctx),
        ),
    )

    fun tapCoordinateActionTypeViewModel(context: Context): PickDisplayCoordinateViewModel.Factory = PickDisplayCoordinateViewModel.Factory(
        ServiceLocator.resourceProvider(context),
    )

    fun swipeCoordinateActionTypeViewModel(context: Context): SwipePickDisplayCoordinateViewModel.Factory = SwipePickDisplayCoordinateViewModel.Factory(
        ServiceLocator.resourceProvider(context),
    )

    fun pinchCoordinateActionTypeViewModel(context: Context): PinchPickDisplayCoordinateViewModel.Factory = PinchPickDisplayCoordinateViewModel.Factory(
        ServiceLocator.resourceProvider(context),
    )

    fun configKeyMapViewModel(
        ctx: Context,
    ): ConfigKeyMapViewModel.Factory = ConfigKeyMapViewModel.Factory(
        UseCases.configKeyMap(ctx),
        TestActionUseCaseImpl(ServiceLocator.accessibilityServiceAdapter(ctx)),
        UseCases.onboarding(ctx),
        (ctx.applicationContext as KeyMapperApp).recordTriggerController,
        UseCases.createKeymapShortcut(ctx),
        UseCases.displayKeyMap(ctx),
        UseCases.createAction(ctx),
        ServiceLocator.resourceProvider(ctx),
        ServiceLocator.purchasingManager(ctx),
        SetupGuiKeyboardUseCaseImpl(
            ServiceLocator.inputMethodAdapter(ctx),
            ServiceLocator.packageManagerAdapter(ctx),
        ),
        FingerprintGesturesSupportedUseCaseImpl(ServiceLocator.settingsRepository(ctx)),
    )

    fun createActionShortcutViewModel(
        ctx: Context,
    ): CreateKeyMapShortcutViewModel.Factory = CreateKeyMapShortcutViewModel.Factory(
        UseCases.configKeyMap(ctx),
        ListKeyMapsUseCaseImpl(
            ServiceLocator.roomKeyMapRepository(ctx),
            ServiceLocator.groupRepository(ctx),
            ServiceLocator.floatingButtonRepository(ctx),
            ServiceLocator.fileAdapter(ctx),
            ServiceLocator.backupManager(ctx),
            ServiceLocator.resourceProvider(ctx),
            UseCases.displayKeyMap(ctx),
        ),
        UseCases.createKeymapShortcut(ctx),
        ServiceLocator.resourceProvider(ctx),
    )

    fun homeViewModel(ctx: Context): HomeViewModel.Factory = HomeViewModel.Factory(
        ListKeyMapsUseCaseImpl(
            ServiceLocator.roomKeyMapRepository(ctx),
            ServiceLocator.groupRepository(ctx),
            ServiceLocator.floatingButtonRepository(ctx),
            ServiceLocator.fileAdapter(ctx),
            ServiceLocator.backupManager(ctx),
            ServiceLocator.resourceProvider(ctx),
            UseCases.displayKeyMap(ctx),
        ),
        UseCases.pauseKeyMaps(ctx),
        BackupRestoreMappingsUseCaseImpl(
            ServiceLocator.fileAdapter(ctx),
            ServiceLocator.backupManager(ctx),
        ),
        ShowHomeScreenAlertsUseCaseImpl(
            ServiceLocator.settingsRepository(ctx),
            ServiceLocator.permissionAdapter(ctx),
            ServiceLocator.accessibilityServiceAdapter(ctx),
            UseCases.pauseKeyMaps(ctx),
        ),
        UseCases.onboarding(ctx),
        ServiceLocator.resourceProvider(ctx),
        SetupGuiKeyboardUseCaseImpl(
            ServiceLocator.inputMethodAdapter(ctx),
            ServiceLocator.packageManagerAdapter(ctx),
        ),
        SortKeyMapsUseCaseImpl(
            ServiceLocator.settingsRepository(ctx),
            UseCases.displayKeyMap(ctx),
        ),
        UseCases.listFloatingLayouts(ctx),
        ShowInputMethodPickerUseCaseImpl(ServiceLocator.inputMethodAdapter(ctx)),
    )

    fun settingsViewModel(context: Context): SettingsViewModel.Factory = SettingsViewModel.Factory(
        ConfigSettingsUseCaseImpl(
            ServiceLocator.settingsRepository(context),
            ServiceLocator.permissionAdapter(context),
            ServiceLocator.inputMethodAdapter(context),
            ServiceLocator.soundsManager(context),
            ServiceLocator.suAdapter(context),
            ServiceLocator.packageManagerAdapter(context),
            ServiceLocator.shizukuAdapter(context),
            ServiceLocator.devicesAdapter(context),
        ),
        ServiceLocator.resourceProvider(context),
    )

    fun accessibilityServiceController(
        service: MyAccessibilityService,
        keyEventRelayService: KeyEventRelayServiceWrapper,
    ): AccessibilityServiceController = AccessibilityServiceController(
        coroutineScope = service.lifecycleScope,
        accessibilityService = service,
        inputEvents = ServiceLocator.accessibilityServiceAdapter(service).eventsToService,
        outputEvents = ServiceLocator.accessibilityServiceAdapter(service).eventReceiver,
        detectConstraintsUseCase = UseCases.detectConstraints(service),
        performActionsUseCase = UseCases.performActions(
            ctx = service,
            service = service,
            keyEventRelayService = keyEventRelayService,
        ),
        detectKeyMapsUseCase = UseCases.detectKeyMaps(
            ctx = service,
            service = service,
            keyEventRelayService = keyEventRelayService,
        ),
        fingerprintGesturesSupportedUseCase = UseCases.fingerprintGesturesSupported(service),
        pauseKeyMapsUseCase = UseCases.pauseKeyMaps(service),
        devicesAdapter = ServiceLocator.devicesAdapter(service),
        suAdapter = ServiceLocator.suAdapter(service),
        rerouteKeyEventsUseCase = UseCases.rerouteKeyEvents(
            ctx = service,
            keyEventRelayService = keyEventRelayService,
        ),
        inputMethodAdapter = ServiceLocator.inputMethodAdapter(service),
        settingsRepository = ServiceLocator.settingsRepository(service),
        nodeRepository = ServiceLocator.accessibilityNodeRepository(service),
    )

    fun chooseBluetoothDeviceViewModel(ctx: Context): ChooseBluetoothDeviceViewModel.Factory = ChooseBluetoothDeviceViewModel.Factory(
        ChooseBluetoothDeviceUseCaseImpl(
            ServiceLocator.devicesAdapter(ctx),
            ServiceLocator.permissionAdapter(ctx),
        ),
        ServiceLocator.resourceProvider(ctx),
    )

    fun logViewModel(ctx: Context): LogViewModel.Factory = LogViewModel.Factory(
        DisplayLogUseCaseImpl(
            ServiceLocator.logRepository(ctx),
            ServiceLocator.resourceProvider(ctx),
            ServiceLocator.clipboardAdapter(ctx),
            ServiceLocator.fileAdapter(ctx),
        ),
        ServiceLocator.resourceProvider(ctx),
    )

    fun interactUiElementViewModel(
        ctx: Context,
    ): InteractUiElementViewModel.Factory = InteractUiElementViewModel.Factory(
        (ctx.applicationContext as KeyMapperApp).interactUiElementController,
        resourceProvider = ServiceLocator.resourceProvider(ctx),
    )
}
