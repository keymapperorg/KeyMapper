package io.github.sds100.keymapper.system

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.airplanemode.AndroidAirplaneModeAdapter
import io.github.sds100.keymapper.system.airplanemode.AirplaneModeAdapter
import io.github.sds100.keymapper.system.apps.AndroidAppShortcutAdapter
import io.github.sds100.keymapper.system.apps.AndroidPackageManagerAdapter
import io.github.sds100.keymapper.system.apps.AppShortcutAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.bluetooth.AndroidBluetoothAdapter
import io.github.sds100.keymapper.system.bluetooth.BluetoothAdapter
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
import io.github.sds100.keymapper.system.inputmethod.AndroidInputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
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
import io.github.sds100.keymapper.system.notifications.AndroidNotificationAdapter
import io.github.sds100.keymapper.system.notifications.NotificationAdapter
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.phone.AndroidPhoneAdapter
import io.github.sds100.keymapper.system.phone.PhoneAdapter
import io.github.sds100.keymapper.system.popup.AndroidToastAdapter
import io.github.sds100.keymapper.system.popup.PopupMessageAdapter
import io.github.sds100.keymapper.system.power.AndroidPowerAdapter
import io.github.sds100.keymapper.system.power.PowerAdapter
import io.github.sds100.keymapper.system.ringtones.AndroidRingtoneAdapter
import io.github.sds100.keymapper.system.ringtones.RingtoneAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.root.SuAdapterImpl
import io.github.sds100.keymapper.system.url.AndroidOpenUrlAdapter
import io.github.sds100.keymapper.system.url.OpenUrlAdapter
import io.github.sds100.keymapper.system.vibrator.AndroidVibratorAdapter
import io.github.sds100.keymapper.system.vibrator.VibratorAdapter
import io.github.sds100.keymapper.system.volume.AndroidVolumeAdapter
import io.github.sds100.keymapper.system.volume.VolumeAdapter
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemHiltModule {
    @Singleton
    @Binds
    abstract fun provideLockscreenAdapter(impl: AndroidLockScreenAdapter): LockScreenAdapter

    @Singleton
    @Binds
    abstract fun provideAirplaneModeAdapter(impl: AndroidAirplaneModeAdapter): AirplaneModeAdapter

    @Singleton
    @Binds
    abstract fun provideAppShortcutAdapter(impl: AndroidAppShortcutAdapter): AppShortcutAdapter

    @Singleton
    @Binds
    abstract fun providePackageManagerAdapter(impl: AndroidPackageManagerAdapter): PackageManagerAdapter

    @Singleton
    @Binds
    abstract fun provideBluetoothAdapter(impl: AndroidBluetoothAdapter): BluetoothAdapter

    @Singleton
    @Binds
    abstract fun provideCameraAdapter(impl: AndroidCameraAdapter): CameraAdapter

    @Singleton
    @Binds
    abstract fun provideClipboardAdapter(impl: AndroidClipboardAdapter): ClipboardAdapter

    @Singleton
    @Binds
    abstract fun provideDevicesAdapter(impl: AndroidDevicesAdapter): DevicesAdapter

    @Singleton
    @Binds
    abstract fun provideDisplayAdapter(impl: AndroidDisplayAdapter): DisplayAdapter

    @Singleton
    @Binds
    abstract fun provideFileAdapter(impl: AndroidFileAdapter): FileAdapter

    @Singleton
    @Binds
    abstract fun provideInputMethodAdapter(impl: AndroidInputMethodAdapter): InputMethodAdapter

    @Singleton
    @Binds
    abstract fun provideIntentAdapter(impl: IntentAdapterImpl): IntentAdapter

    @Singleton
    @Binds
    abstract fun provideLeanbackAdapter(impl: LeanbackAdapterImpl): LeanbackAdapter

    @Singleton
    @Binds
    abstract fun provideMediaAdapter(impl: AndroidMediaAdapter): MediaAdapter

    @Singleton
    @Binds
    abstract fun provideNetworkAdapter(impl: AndroidNetworkAdapter): NetworkAdapter

    @Singleton
    @Binds
    abstract fun provideNfcAdapter(impl: AndroidNfcAdapter): NfcAdapter

    @Singleton
    @Binds
    abstract fun provideNotificationAdapter(impl: AndroidNotificationAdapter): NotificationAdapter

    @Singleton
    @Binds
    abstract fun providePermissionAdapter(impl: AndroidPermissionAdapter): PermissionAdapter

    @Singleton
    @Binds
    abstract fun providePhoneAdapter(impl: AndroidPhoneAdapter): PhoneAdapter

    @Singleton
    @Binds
    abstract fun providePopupMessageAdapter(impl: AndroidToastAdapter): PopupMessageAdapter

    @Singleton
    @Binds
    abstract fun providePowerAdapter(impl: AndroidPowerAdapter): PowerAdapter

    @Singleton
    @Binds
    abstract fun provideRingtoneAdapter(impl: AndroidRingtoneAdapter): RingtoneAdapter

    @Singleton
    @Binds
    abstract fun provideSuAdapter(impl: SuAdapterImpl): SuAdapter

    @Singleton
    @Binds
    abstract fun provideOpenUrlAdapter(impl: AndroidOpenUrlAdapter): OpenUrlAdapter

    @Singleton
    @Binds
    abstract fun provideVibratorAdapter(impl: AndroidVibratorAdapter): VibratorAdapter

    @Singleton
    @Binds
    abstract fun provideVolumeAdapter(impl: AndroidVolumeAdapter): VolumeAdapter
}
