package io.github.sds100.keymapper

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.google.android.material.color.DynamicColors
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.logging.KeyMapperLoggingTree
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerController
import io.github.sds100.keymapper.settings.ThemeUtils
import io.github.sds100.keymapper.shizuku.ShizukuAdapterImpl
import io.github.sds100.keymapper.system.AndroidSystemFeatureAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.airplanemode.AndroidAirplaneModeAdapter
import io.github.sds100.keymapper.system.apps.AndroidAppShortcutAdapter
import io.github.sds100.keymapper.system.apps.AndroidPackageManagerAdapter
import io.github.sds100.keymapper.system.bluetooth.AndroidBluetoothAdapter
import io.github.sds100.keymapper.system.camera.AndroidCameraAdapter
import io.github.sds100.keymapper.system.clipboard.AndroidClipboardAdapter
import io.github.sds100.keymapper.system.devices.AndroidDevicesAdapter
import io.github.sds100.keymapper.system.display.AndroidDisplayAdapter
import io.github.sds100.keymapper.system.files.AndroidFileAdapter
import io.github.sds100.keymapper.system.inputmethod.AndroidInputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.AutoSwitchImeController
import io.github.sds100.keymapper.system.inputmethod.ShowHideInputMethodUseCaseImpl
import io.github.sds100.keymapper.system.intents.IntentAdapterImpl
import io.github.sds100.keymapper.system.leanback.LeanbackAdapterImpl
import io.github.sds100.keymapper.system.lock.AndroidLockScreenAdapter
import io.github.sds100.keymapper.system.media.AndroidMediaAdapter
import io.github.sds100.keymapper.system.network.AndroidNetworkAdapter
import io.github.sds100.keymapper.system.nfc.AndroidNfcAdapter
import io.github.sds100.keymapper.system.notifications.AndroidNotificationAdapter
import io.github.sds100.keymapper.system.notifications.ManageNotificationsUseCaseImpl
import io.github.sds100.keymapper.system.notifications.NotificationController
import io.github.sds100.keymapper.system.notifications.NotificationReceiverAdapter
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.AutoGrantPermissionController
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.phone.AndroidPhoneAdapter
import io.github.sds100.keymapper.system.popup.AndroidToastAdapter
import io.github.sds100.keymapper.system.power.AndroidPowerAdapter
import io.github.sds100.keymapper.system.root.SuAdapterImpl
import io.github.sds100.keymapper.system.ui.RecordUiElementsController
import io.github.sds100.keymapper.system.url.AndroidOpenUrlAdapter
import io.github.sds100.keymapper.system.vibrator.AndroidVibratorAdapter
import io.github.sds100.keymapper.system.volume.AndroidVolumeAdapter
import io.github.sds100.keymapper.util.ui.ResourceProviderImpl
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import splitties.toast.toast
import timber.log.Timber
import java.util.Calendar

/**
 * Created by sds100 on 19/05/2020.
 */
class KeyMapperApp : MultiDexApplication() {
    val appCoroutineScope = MainScope()

    val notificationAdapter by lazy { AndroidNotificationAdapter(this, appCoroutineScope) }

    lateinit var notificationController: NotificationController
    lateinit var autoSwitchImeController: AutoSwitchImeController

    val resourceProvider by lazy { ResourceProviderImpl(this, appCoroutineScope) }

    val bluetoothMonitor by lazy { AndroidBluetoothAdapter(this, appCoroutineScope) }

    val packageManagerAdapter by lazy {
        AndroidPackageManagerAdapter(
            this,
            appCoroutineScope,
        )
    }

    val inputMethodAdapter by lazy {
        AndroidInputMethodAdapter(
            this,
            appCoroutineScope,
            accessibilityServiceAdapter,
            permissionAdapter,
            suAdapter,
        )
    }
    val devicesAdapter by lazy {
        AndroidDevicesAdapter(
            this,
            bluetoothMonitor,
            permissionAdapter,
            appCoroutineScope,
        )
    }
    val cameraAdapter by lazy { AndroidCameraAdapter(this) }
    val permissionAdapter by lazy {
        AndroidPermissionAdapter(
            this,
            appCoroutineScope,
            suAdapter,
            notificationReceiverAdapter,
        )
    }

    val systemFeatureAdapter by lazy { AndroidSystemFeatureAdapter(this) }
    val accessibilityServiceAdapter by lazy { AccessibilityServiceAdapter(this, appCoroutineScope) }
    val notificationReceiverAdapter by lazy { NotificationReceiverAdapter(this, appCoroutineScope) }
    val appShortcutAdapter by lazy { AndroidAppShortcutAdapter(this) }
    val fileAdapter by lazy { AndroidFileAdapter(this) }
    val popupMessageAdapter by lazy { AndroidToastAdapter(this) }
    val vibratorAdapter by lazy { AndroidVibratorAdapter(this) }
    val displayAdapter by lazy { AndroidDisplayAdapter(this) }
    val audioAdapter by lazy { AndroidVolumeAdapter(this) }
    val suAdapter by lazy {
        SuAdapterImpl(
            appCoroutineScope,
            ServiceLocator.settingsRepository(this),
        )
    }
    val phoneAdapter by lazy { AndroidPhoneAdapter(this, appCoroutineScope) }
    val intentAdapter by lazy { IntentAdapterImpl(this) }
    val mediaAdapter by lazy { AndroidMediaAdapter(this) }
    val lockScreenAdapter by lazy { AndroidLockScreenAdapter(this) }
    val airplaneModeAdapter by lazy { AndroidAirplaneModeAdapter(this, suAdapter) }
    val networkAdapter by lazy { AndroidNetworkAdapter(this, suAdapter) }
    val nfcAdapter by lazy { AndroidNfcAdapter(this, suAdapter) }
    val openUrlAdapter by lazy { AndroidOpenUrlAdapter(this) }
    val clipboardAdapter by lazy { AndroidClipboardAdapter(this) }
    val shizukuAdapter by lazy { ShizukuAdapterImpl(appCoroutineScope, packageManagerAdapter) }
    val leanbackAdapter by lazy { LeanbackAdapterImpl(this) }
    val powerAdapter by lazy { AndroidPowerAdapter(this) }

    val recordTriggerController by lazy {
        RecordTriggerController(appCoroutineScope, accessibilityServiceAdapter)
    }

    val recordUiElementsController by lazy {
        RecordUiElementsController(appCoroutineScope, accessibilityServiceAdapter)
    }

    val autoGrantPermissionController by lazy {
        AutoGrantPermissionController(
            appCoroutineScope,
            permissionAdapter,
            popupMessageAdapter,
            resourceProvider,
        )
    }

    private val loggingTree by lazy {
        KeyMapperLoggingTree(
            appCoroutineScope,
            ServiceLocator.settingsRepository(this),
            ServiceLocator.logRepository(this),
        )
    }

    private val processLifecycleOwner by lazy { ProcessLifecycleOwner.get() }

    override fun onCreate() {
        val priorExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            // log in a blocking manner and always log regardless of whether the setting is turned on
            val entry = LogEntryEntity(
                id = 0,
                time = Calendar.getInstance().timeInMillis,
                severity = LogEntryEntity.SEVERITY_ERROR,
                message = exception.stackTraceToString(),
            )

            runBlocking {
                ServiceLocator.logRepository(this@KeyMapperApp).insertSuspend(entry)
            }

            priorExceptionHandler?.uncaughtException(thread, exception)
        }

        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }

        ServiceLocator.settingsRepository(this).get(Keys.darkTheme)
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

        if (BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "debug_release") {
            Timber.plant(Timber.DebugTree())
        }

        Timber.plant(loggingTree)

        notificationController = NotificationController(
            appCoroutineScope,
            ManageNotificationsUseCaseImpl(
                ServiceLocator.settingsRepository(this),
                notificationAdapter,
                suAdapter,
            ),
            UseCases.pauseMappings(this),
            UseCases.showImePicker(this),
            UseCases.controlAccessibilityService(this),
            UseCases.toggleCompatibleIme(this),
            ShowHideInputMethodUseCaseImpl(ServiceLocator.accessibilityServiceAdapter(this)),
            UseCases.fingerprintGesturesSupported(this),
            UseCases.onboarding(this),
            ServiceLocator.resourceProvider(this),
        )

        autoSwitchImeController = AutoSwitchImeController(
            appCoroutineScope,
            ServiceLocator.settingsRepository(this),
            ServiceLocator.inputMethodAdapter(this),
            UseCases.pauseMappings(this),
            devicesAdapter,
            popupMessageAdapter,
            resourceProvider,
            ServiceLocator.accessibilityServiceAdapter(this),
        )

        processLifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                // when the user returns to the app let everything know that the permissions could have changed
                permissionAdapter.onPermissionsChanged()
                accessibilityServiceAdapter.updateWhetherServiceIsEnabled()
                notificationController.onOpenApp()

                if (BuildConfig.DEBUG && permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)) {
                    accessibilityServiceAdapter.start()
                }
            }
        })

        appCoroutineScope.launch {
            notificationController.openApp.collectLatest { intentAction ->
                Intent(this@KeyMapperApp, SplashActivity::class.java).apply {
                    action = intentAction
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    startActivity(this)
                }
            }
        }

        notificationController.showToast.onEach {
            toast(it)
        }.launchIn(appCoroutineScope)

        autoGrantPermissionController.start()
    }
}
