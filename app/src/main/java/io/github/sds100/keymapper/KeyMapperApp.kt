package io.github.sds100.keymapper

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp
import io.github.sds100.keymapper.base.data.entities.LogEntryEntity
import io.github.sds100.keymapper.base.logging.KeyMapperLoggingTree
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.settings.ThemeUtils
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.apps.AndroidPackageManagerAdapter
import io.github.sds100.keymapper.system.devices.AndroidDevicesAdapter
import io.github.sds100.keymapper.system.inputmethod.AutoSwitchImeController
import io.github.sds100.keymapper.system.inputmethod.ShowHideInputMethodUseCaseImpl
import io.github.sds100.keymapper.system.notifications.AndroidNotificationAdapter
import io.github.sds100.keymapper.system.notifications.ManageNotificationsUseCaseImpl
import io.github.sds100.keymapper.system.notifications.NotificationController
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.AutoGrantPermissionController
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.root.SuAdapterImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import splitties.toast.toast
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

@SuppressLint("LogNotTimber")
@HiltAndroidApp
class KeyMapperApp : MultiDexApplication() {
    private val tag = KeyMapperApp::class.simpleName

    @Inject
    private lateinit var appCoroutineScope: CoroutineScope

    @Inject
    private lateinit var notificationAdapter: AndroidNotificationAdapter

    lateinit var notificationController: NotificationController
    lateinit var autoSwitchImeController: AutoSwitchImeController

    @Inject
    private lateinit var packageManagerAdapter: AndroidPackageManagerAdapter

    @Inject
    private lateinit var devicesAdapter: AndroidDevicesAdapter

    @Inject
    private lateinit var permissionAdapter: AndroidPermissionAdapter

    @Inject
    private lateinit var accessibilityServiceAdapter: AccessibilityServiceAdapter

    @Inject
    private lateinit var suAdapter: SuAdapterImpl

    @Inject
    private lateinit var autoGrantPermissionController: AutoGrantPermissionController

    @Inject
    private lateinit var loggingTree: KeyMapperLoggingTree

    private val processLifecycleOwner by lazy { ProcessLifecycleOwner.get() }

    private val userManager: UserManager? by lazy { getSystemService<UserManager>() }

    private val initLock: Any = Any()
    private var initialized = false

    override fun onCreate() {
        val priorExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        Log.i(tag, "KeyMapperApp: OnCreate")

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && userManager?.isUserUnlocked == false) {
            Log.i(tag, "KeyMapperApp: Delay init because locked.")
            // If the device is still encrypted and locked do not initialize anything that
            // may potentially need the encrypted app storage like databases.
            return
        }

        synchronized(initLock) {
            init()
            initialized = true
        }
    }

    fun onBootUnlocked() {
        synchronized(initLock) {
            if (!initialized) {
                init()
            }
            initialized = true
        }
    }

    private fun init() {
        Log.i(tag, "KeyMapperApp: Init")

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
                permissionAdapter,
            ),
            UseCases.pauseKeyMaps(this),
            UseCases.showImePicker(this),
            UseCases.controlAccessibilityService(this),
            UseCases.toggleCompatibleIme(this),
            ShowHideInputMethodUseCaseImpl(ServiceLocator.accessibilityServiceAdapter(this)),
            UseCases.onboarding(this),
            ServiceLocator.resourceProvider(this),
        )

        autoSwitchImeController = AutoSwitchImeController(
            appCoroutineScope,
            ServiceLocator.settingsRepository(this),
            ServiceLocator.inputMethodAdapter(this),
            UseCases.pauseKeyMaps(this),
            devicesAdapter,
            popupMessageAdapter,
            resourceProvider,
            ServiceLocator.accessibilityServiceAdapter(this),
        )

        processLifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                // when the user returns to the app let everything know that the permissions could have changed
                notificationController.onOpenApp()

                if (BuildConfig.DEBUG && permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)) {
                    accessibilityServiceAdapter.start()
                }
            }
        })

        appCoroutineScope.launch {
            notificationController.openApp.collectLatest { intentAction ->
                Intent(this@KeyMapperApp, MainActivity::class.java).apply {
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
