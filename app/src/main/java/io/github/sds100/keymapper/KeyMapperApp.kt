package io.github.sds100.keymapper

import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerController
import io.github.sds100.keymapper.settings.ThemeUtils
import io.github.sds100.keymapper.system.AndroidSystemFeatureAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.airplanemode.AndroidAirplaneModeAdapter
import io.github.sds100.keymapper.system.apps.AndroidAppShortcutAdapter
import io.github.sds100.keymapper.system.apps.AndroidPackageManagerAdapter
import io.github.sds100.keymapper.system.bluetooth.AndroidBluetoothAdapter
import io.github.sds100.keymapper.system.camera.AndroidCameraAdapter
import io.github.sds100.keymapper.system.devices.AndroidDevicesAdapter
import io.github.sds100.keymapper.system.display.AndroidDisplayAdapter
import io.github.sds100.keymapper.system.files.AndroidFileAdapter
import io.github.sds100.keymapper.system.inputmethod.AndroidInputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.AutoSwitchImeController
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.inputmethod.ShowHideInputMethodUseCaseImpl
import io.github.sds100.keymapper.system.intents.IntentAdapterImpl
import io.github.sds100.keymapper.system.lock.AndroidLockScreenAdapter
import io.github.sds100.keymapper.system.media.AndroidMediaAdapter
import io.github.sds100.keymapper.system.network.AndroidNetworkAdapter
import io.github.sds100.keymapper.system.nfc.AndroidNfcAdapter
import io.github.sds100.keymapper.system.notifications.AndroidNotificationAdapter
import io.github.sds100.keymapper.system.notifications.ManageNotificationsUseCaseImpl
import io.github.sds100.keymapper.system.notifications.NotificationController
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.phone.AndroidPhoneAdapter
import io.github.sds100.keymapper.system.popup.AndroidToastAdapter
import io.github.sds100.keymapper.system.root.SuAdapterImpl
import io.github.sds100.keymapper.system.url.AndroidOpenUrlAdapter
import io.github.sds100.keymapper.system.vibrator.AndroidVibratorAdapter
import io.github.sds100.keymapper.system.volume.AndroidVolumeAdapter
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.ResourceProviderImpl
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import splitties.toast.toast
import timber.log.Timber

/**
 * Created by sds100 on 19/05/2020.
 */
class KeyMapperApp : MultiDexApplication() {
    val appCoroutineScope = MainScope()

    val notificationAdapter by lazy { AndroidNotificationAdapter(this, appCoroutineScope) }

    lateinit var notificationController: NotificationController

    lateinit var autoSwitchImeController: AutoSwitchImeController

    val resourceProvider by lazy { ResourceProviderImpl(this) }

    val bluetoothMonitor by lazy { AndroidBluetoothAdapter(this, appCoroutineScope) }

    val packageManagerAdapter by lazy {
        AndroidPackageManagerAdapter(
            this,
            appCoroutineScope
        )
    }

    val inputMethodAdapter by lazy {
        AndroidInputMethodAdapter(
            this,
            serviceAdapter,
            permissionAdapter,
            suAdapter
        )
    }
    val devicesAdapter by lazy {
        AndroidDevicesAdapter(
            this,
            bluetoothMonitor,
            appCoroutineScope
        )
    }
    val cameraAdapter by lazy { AndroidCameraAdapter(this) }
    val permissionAdapter by lazy { AndroidPermissionAdapter(this, appCoroutineScope, suAdapter) }
    val systemFeatureAdapter by lazy { AndroidSystemFeatureAdapter(this) }
    val serviceAdapter by lazy { AccessibilityServiceAdapter(this, appCoroutineScope) }
    val appShortcutAdapter by lazy { AndroidAppShortcutAdapter(this) }
    val fileAdapter by lazy { AndroidFileAdapter(this) }
    val popupMessageAdapter by lazy { AndroidToastAdapter(this) }
    val vibratorAdapter by lazy { AndroidVibratorAdapter(this) }
    val displayAdapter by lazy { AndroidDisplayAdapter(this) }
    val audioAdapter by lazy { AndroidVolumeAdapter(this) }
    val suAdapter by lazy {
        SuAdapterImpl(
            appCoroutineScope,
            ServiceLocator.preferenceRepository(this)
        )
    }
    val phoneAdapter by lazy { AndroidPhoneAdapter(this) }
    val intentAdapter by lazy { IntentAdapterImpl(this) }
    val mediaAdapter by lazy { AndroidMediaAdapter(this, permissionAdapter) }
    val lockScreenAdapter by lazy { AndroidLockScreenAdapter(this) }
    val airplaneModeAdapter by lazy { AndroidAirplaneModeAdapter(this, suAdapter) }
    val networkAdapter by lazy { AndroidNetworkAdapter(this, suAdapter) }
    val nfcAdapter by lazy { AndroidNfcAdapter(this, suAdapter) }
    val openUrlAdapter by lazy { AndroidOpenUrlAdapter(this) }

    val recordTriggerController by lazy {
        RecordTriggerController(appCoroutineScope, serviceAdapter)
    }

    private val processLifecycleOwner by lazy { ProcessLifecycleOwner.get() }

    override fun onCreate() {

        ServiceLocator.preferenceRepository(this).get(Keys.darkTheme)
            .map { it?.toIntOrNull() }
            .map {
                when (it) {
                    ThemeUtils.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    ThemeUtils.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }
            .onEach { mode -> AppCompatDelegate.setDefaultNightMode(mode) }
            .launchIn(appCoroutineScope)

        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        notificationController = NotificationController(
            appCoroutineScope,
            ManageNotificationsUseCaseImpl(
                ServiceLocator.preferenceRepository(this),
                notificationAdapter,
                suAdapter
            ),
            UseCases.pauseMappings(this),
            UseCases.showImePicker(this),
            UseCases.controlAccessibilityService(this),
            UseCases.toggleCompatibleIme(this),
            ShowHideInputMethodUseCaseImpl(ServiceLocator.serviceAdapter(this)),
            UseCases.fingerprintGesturesSupported(this),
            UseCases.onboarding(this),
            ServiceLocator.resourceProvider(this)
        )

        autoSwitchImeController = AutoSwitchImeController(
            appCoroutineScope,
            ServiceLocator.preferenceRepository(this),
            ServiceLocator.inputMethodAdapter(this),
            UseCases.pauseMappings(this),
            devicesAdapter,
            popupMessageAdapter,
            resourceProvider
        )

        processLifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                //when the user returns to the app let everything know that the permissions could have changed
                permissionAdapter.onPermissionsChanged()
                serviceAdapter.updateWhetherServiceIsEnabled()
                notificationController.onOpenApp()

                if (BuildConfig.DEBUG && permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)) {
                    serviceAdapter.enableService()
                }
            }
        })

        appCoroutineScope.launch {
            notificationController.openApp.collectLatest {
                Intent(this@KeyMapperApp, SplashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    startActivity(this)
                }
            }
        }

        notificationController.showToast.onEach {
            toast(it)
        }.launchIn(appCoroutineScope)
    }
}