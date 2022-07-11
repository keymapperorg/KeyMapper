package io.github.sds100.keymapper

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.logging.KeyMapperLoggingTree
import io.github.sds100.keymapper.logging.LogRepository
import io.github.sds100.keymapper.settings.ThemeUtils
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapterImpl
import io.github.sds100.keymapper.system.inputmethod.AndroidInputMethodAdapter
import io.github.sds100.keymapper.system.notifications.NotificationController
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.AutoGrantPermissionController
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import splitties.toast.toast
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Created by sds100 on 19/05/2020.
 */
@HiltAndroidApp
class KeyMapperApp : MultiDexApplication() {

    @Inject
    lateinit var coroutineScope: CoroutineScope
    @Inject
    lateinit var notificationController: NotificationController

    @Inject
    lateinit var logRepository: LogRepository

    @Inject
    lateinit var settingsRepository: PreferenceRepository

    @Inject
    lateinit var loggingTree: KeyMapperLoggingTree
    @Inject
    lateinit var permissionAdapter: AndroidPermissionAdapter
    @Inject
    lateinit var accessibilityServiceAdapter: AccessibilityServiceAdapterImpl
    @Inject
    lateinit var autoGrantPermissionController: AutoGrantPermissionController
    @Inject
    lateinit var inputMethodAdapter: AndroidInputMethodAdapter
    @Inject
    lateinit var resourceProvider: ResourceProvider

    private val processLifecycleOwner by lazy { ProcessLifecycleOwner.get() }

    override fun onCreate() {
        val priorExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            //log in a blocking manner and always log regardless of whether the setting is turned on
            val entry = LogEntryEntity(
                id = 0,
                time = Calendar.getInstance().timeInMillis,
                    severity = LogEntryEntity.SEVERITY_ERROR,
                    message = exception.stackTraceToString()
            )

            runBlocking {
                logRepository.insertSuspend(entry)
            }

            priorExceptionHandler?.uncaughtException(thread, exception)
        }

        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }

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
            .launchIn(coroutineScope)

        if (BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "debug_release") {
            Timber.plant(Timber.DebugTree())
        }

        Timber.plant(loggingTree)

        processLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)

                //when the user returns to the app let everything know that the permissions could have changed
                permissionAdapter.onPermissionsChanged()
                accessibilityServiceAdapter.updateWhetherServiceIsEnabled()
                notificationController.onOpenApp()

                if (BuildConfig.DEBUG && permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)) {
                    accessibilityServiceAdapter.start()
                }
            }
        })

        coroutineScope.launch {
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
        }.launchIn(coroutineScope)

        autoGrantPermissionController.start()
    }
}