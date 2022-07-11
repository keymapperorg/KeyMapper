package io.github.sds100.keymapper.system

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.logging.DisplayLogUseCase
import io.github.sds100.keymapper.logging.DisplayLogUseCaseImpl
import io.github.sds100.keymapper.shizuku.ShizukuAdapter
import io.github.sds100.keymapper.shizuku.ShizukuAdapterImpl
import io.github.sds100.keymapper.shizuku.ShizukuInputEventInjector
import io.github.sds100.keymapper.shizuku.ShizukuInputEventInjectorImpl
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapterImpl
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCaseImpl
import io.github.sds100.keymapper.system.airplanemode.AirplaneModeAdapter
import io.github.sds100.keymapper.system.airplanemode.AndroidAirplaneModeAdapter
import io.github.sds100.keymapper.system.apps.*
import io.github.sds100.keymapper.system.bluetooth.AndroidBluetoothAdapter
import io.github.sds100.keymapper.system.bluetooth.BluetoothAdapter
import io.github.sds100.keymapper.system.bluetooth.ChooseBluetoothDeviceUseCase
import io.github.sds100.keymapper.system.bluetooth.ChooseBluetoothDeviceUseCaseImpl
import io.github.sds100.keymapper.system.camera.AndroidCameraAdapter
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.clipboard.AndroidClipboardAdapter
import io.github.sds100.keymapper.system.clipboard.ClipboardAdapter
import io.github.sds100.keymapper.system.devices.AndroidDevicesAdapter
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.AndroidDisplayAdapter
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.files.AndroidFileAdapter
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.inputmethod.*
import io.github.sds100.keymapper.system.intents.IntentAdapter
import io.github.sds100.keymapper.system.intents.IntentAdapterImpl
import io.github.sds100.keymapper.system.leanback.LeanbackAdapter
import io.github.sds100.keymapper.system.leanback.LeanbackAdapterImpl
import io.github.sds100.keymapper.system.lock.AndroidLockScreenAdapter
import io.github.sds100.keymapper.system.lock.LockScreenAdapter
import io.github.sds100.keymapper.system.media.AndroidMediaAdapter
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.system.network.AndroidNetworkAdapter
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.nfc.AndroidNfcAdapter
import io.github.sds100.keymapper.system.nfc.NfcAdapter
import io.github.sds100.keymapper.system.notifications.*
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.system.phone.AndroidPhoneAdapter
import io.github.sds100.keymapper.system.phone.PhoneAdapter
import io.github.sds100.keymapper.system.popup.AndroidToastAdapter
import io.github.sds100.keymapper.system.popup.ToastAdapter
import io.github.sds100.keymapper.system.power.AndroidPowerAdapter
import io.github.sds100.keymapper.system.power.PowerAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.root.SuAdapterImpl
import io.github.sds100.keymapper.system.shell.ShellAdapter
import io.github.sds100.keymapper.system.url.AndroidOpenUrlAdapter
import io.github.sds100.keymapper.system.url.OpenUrlAdapter
import io.github.sds100.keymapper.system.vibrator.AndroidVibratorAdapter
import io.github.sds100.keymapper.system.vibrator.VibratorAdapter
import io.github.sds100.keymapper.system.volume.AndroidVolumeAdapter
import io.github.sds100.keymapper.system.volume.VolumeAdapter
import javax.inject.Singleton

/**
 * Created by sds100 on 28/06/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SystemModule {

    @Binds
    abstract fun bindDisplayAppsUseCase(impl: DisplayAppsUseCaseImpl): DisplayAppsUseCase

    @Binds
    abstract fun bindDisplayAppShortcutsUseCase(impl: DisplayAppShortcutsUseCaseImpl): DisplayAppShortcutsUseCase

    @Binds
    abstract fun bindChooseBluetoothDeviceUseCase(impl: ChooseBluetoothDeviceUseCaseImpl): ChooseBluetoothDeviceUseCase

    @Binds
    abstract fun bindManageNotificationsUseCase(impl: ManageNotificationsUseCaseImpl): ManageNotificationsUseCase

    @Binds
    abstract fun bindShowInputMethodPickerUseCase(impl: ShowInputMethodPickerUseCaseImpl): ShowInputMethodPickerUseCase

    @Binds
    abstract fun bindControlAccessibilityServiceUseCase(impl: ControlAccessibilityServiceUseCaseImpl): ControlAccessibilityServiceUseCase

    @Binds
    abstract fun bindToggleCompatibleImeUseCase(impl: ToggleCompatibleImeUseCaseImpl): ToggleCompatibleImeUseCase

    @Binds
    abstract fun bindShowHideInputMethodUseCase(impl: ShowHideInputMethodUseCaseImpl): ShowHideInputMethodUseCase

    @Binds
    abstract fun bindKeyMapperImeMessenger(impl: KeyMapperImeMessengerImpl): KeyMapperImeMessenger

    @Binds
    abstract fun bindShizukuInputEventInjector(impl: ShizukuInputEventInjectorImpl): ShizukuInputEventInjector

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

    @Binds
    @Singleton
    abstract fun bindAccessibilityServiceAdapter(impl: AccessibilityServiceAdapterImpl): AccessibilityServiceAdapter

    @Binds
    @Singleton
    abstract fun bindPermissionAdapter(impl: AndroidPermissionAdapter): PermissionAdapter

    @Binds
    @Singleton
    abstract fun bindFileAdapter(impl: AndroidFileAdapter): FileAdapter

    @Binds
    @Singleton
    abstract fun bindLeanbackAdapter(impl: LeanbackAdapterImpl): LeanbackAdapter

    @Binds
    @Singleton
    abstract fun bindPackageManagerAdapter(impl: AndroidPackageManagerAdapter): PackageManagerAdapter

    @Binds
    @Singleton
    abstract fun bindNotificationReceiverAdapter(impl: NotificationReceiverAdapterImpl): NotificationReceiverAdapter

    @Binds
    @Singleton
    abstract fun bindToastAdapter(impl: AndroidToastAdapter): ToastAdapter

    @Binds
    @Singleton
    abstract fun bindSystemFeatureAdapter(impl: AndroidSystemFeatureAdapter): SystemFeatureAdapter

    @Binds
    @Singleton
    abstract fun bindDevicesAdapter(impl: AndroidDevicesAdapter): DevicesAdapter

    @Binds
    @Singleton
    abstract fun bindBluetoothAdapter(impl: AndroidBluetoothAdapter): BluetoothAdapter

    @Binds
    @Singleton
    abstract fun bindAppShortcutAdapter(impl: AndroidAppShortcutAdapter): AppShortcutAdapter

    @Binds
    @Singleton
    abstract fun bindNetworkAdapter(impl: AndroidNetworkAdapter): NetworkAdapter

    @Binds
    @Singleton
    abstract fun bindCameraAdapter(impl: AndroidCameraAdapter): CameraAdapter

    @Binds
    @Singleton
    abstract fun bindDisplayAdapter(impl: AndroidDisplayAdapter): DisplayAdapter

    @Binds
    @Singleton
    abstract fun bindLockScreenAdapter(impl: AndroidLockScreenAdapter): LockScreenAdapter

    @Binds
    @Singleton
    abstract fun bindPhoneAdapter(impl: AndroidPhoneAdapter): PhoneAdapter

    @Binds
    @Singleton
    abstract fun bindPowerAdapter(impl: AndroidPowerAdapter): PowerAdapter

    @Binds
    @Singleton
    abstract fun bindShellAdapter(impl: Shell): ShellAdapter

    @Binds
    @Singleton
    abstract fun bindIntentAdapter(impl: IntentAdapterImpl): IntentAdapter

    @Binds
    @Singleton
    abstract fun bindVolumeAdapter(impl: AndroidVolumeAdapter): VolumeAdapter

    @Binds
    @Singleton
    abstract fun bindAirplaneModeAdapter(impl: AndroidAirplaneModeAdapter): AirplaneModeAdapter

    @Binds
    @Singleton
    abstract fun bindNfcAdapter(impl: AndroidNfcAdapter): NfcAdapter

    @Binds
    @Singleton
    abstract fun bindOpenUrlAdapter(impl: AndroidOpenUrlAdapter): OpenUrlAdapter

    @Binds
    @Singleton
    abstract fun bindVibratorAdapter(impl: AndroidVibratorAdapter): VibratorAdapter
    
    @Binds
    @Singleton
    abstract fun bindClipboardAdapter(impl: AndroidClipboardAdapter): ClipboardAdapter
}