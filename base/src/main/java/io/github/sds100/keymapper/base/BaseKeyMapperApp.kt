package io.github.sds100.keymapper.base

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import io.github.sds100.keymapper.base.logging.KeyMapperLoggingTree
import io.github.sds100.keymapper.base.settings.ThemeUtils
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityServiceAdapterImpl
import io.github.sds100.keymapper.base.system.inputmethod.AutoSwitchImeController
import io.github.sds100.keymapper.base.system.notifications.NotificationController
import io.github.sds100.keymapper.base.system.permissions.AutoGrantPermissionController
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.data.repositories.LogRepository
import io.github.sds100.keymapper.data.repositories.SettingsPreferenceRepository
import io.github.sds100.keymapper.system.apps.AndroidPackageManagerAdapter
import io.github.sds100.keymapper.system.devices.AndroidDevicesAdapter
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.root.SuAdapterImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

@SuppressLint("LogNotTimber")
abstract class BaseKeyMapperApp : MultiDexApplication() {
    private val tag = BaseKeyMapperApp::class.simpleName

    @Inject
    lateinit var appCoroutineScope: CoroutineScope

    @Inject
    lateinit var notificationController: NotificationController

    @Inject
    lateinit var autoSwitchImeController: AutoSwitchImeController

    @Inject
    lateinit var packageManagerAdapter: AndroidPackageManagerAdapter

    @Inject
    lateinit var devicesAdapter: AndroidDevicesAdapter

    @Inject
    lateinit var permissionAdapter: AndroidPermissionAdapter

    @Inject
    lateinit var accessibilityServiceAdapter: AccessibilityServiceAdapterImpl

    @Inject
    lateinit var suAdapter: SuAdapterImpl

    @Inject
    lateinit var autoGrantPermissionController: AutoGrantPermissionController

    @Inject
    lateinit var loggingTree: KeyMapperLoggingTree

    @Inject
    lateinit var settingsRepository: SettingsPreferenceRepository

    @Inject
    lateinit var logRepository: LogRepository

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
                logRepository.insertSuspend(entry)
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

        settingsRepository.get(Keys.darkTheme)
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

        notificationController.init()

        autoSwitchImeController.init()

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
                Intent(this@BaseKeyMapperApp, getMainActivityClass()).apply {
                    action = intentAction
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    startActivity(this)
                }
            }
        }

        notificationController.showToast.onEach { toast ->
            Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()
        }.launchIn(appCoroutineScope)

        autoGrantPermissionController.start()
    }

    abstract fun getMainActivityClass(): Class<*>
}
