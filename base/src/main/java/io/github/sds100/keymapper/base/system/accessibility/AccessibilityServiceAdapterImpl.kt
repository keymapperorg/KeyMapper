package io.github.sds100.keymapper.base.system.accessibility

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.KeyMapperClassProvider
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.system.JobSchedulerHelper
import io.github.sds100.keymapper.system.SettingsUtils
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityServiceAdapterImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val coroutineScope: CoroutineScope,
    private val permissionAdapter: PermissionAdapter,
    private val buildConfigProvider: BuildConfigProvider,
    private val classProvider: KeyMapperClassProvider,
) : AccessibilityServiceAdapter {

    private val ctx = context.applicationContext
    override val eventReceiver = MutableSharedFlow<AccessibilityServiceEvent>()

    val eventsToService = MutableSharedFlow<AccessibilityServiceEvent>()

    override val state = MutableStateFlow(AccessibilityServiceState.DISABLED)

    init {
        // use job scheduler because there is there is a much shorter delay when the app is in the background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            JobSchedulerHelper.observeEnabledAccessibilityServices(ctx)
        } else {
            val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)

                    coroutineScope.launch {
                        state.value = getState()
                    }
                }
            }

            ctx.contentResolver.registerContentObserver(uri, false, observer)
        }

        coroutineScope.launch {
            state.value = getState()
        }

        eventReceiver.onEach {
            Timber.d("Received event from service: $it")
        }.launchIn(coroutineScope)
    }

    override fun sendAsync(event: AccessibilityServiceEvent) {
        coroutineScope.launch {
            eventsToService.emit(event)
        }
    }

    override suspend fun send(event: AccessibilityServiceEvent): KMResult<*> {
        state.value = getState()

        if (state.value == AccessibilityServiceState.DISABLED) {
            Timber.e("Failed to send event to accessibility service because disabled: $event")
            return KMError.AccessibilityServiceDisabled
        }

        if (state.value == AccessibilityServiceState.CRASHED) {
            Timber.e("Failed to send event to accessibility service because crashed: $event")
            return KMError.AccessibilityServiceCrashed
        }

        coroutineScope.launch {
            eventsToService.emit(event)
        }

        return Success(Unit)
    }

    override fun start(): Boolean {
        if (permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)) {
            enableWithWriteSecureSettings()

            /*
            Turning on the accessibility service doesn't necessarily mean that it is running so
            this will check if it is indeed running and then turn it off and on so that it
            is running again.
             */
            coroutineScope.launch {
                delay(200)

                val key = "check_is_crashed_then_restart"

                // wait to start collecting
                coroutineScope.launch {
                    delay(100)

                    Timber.d("Ping service to check if crashed")
                    eventsToService.emit(AccessibilityServiceEvent.Ping(key))
                }

                val pong: AccessibilityServiceEvent.Pong? = withTimeoutOrNull(2000L) {
                    eventReceiver.first { it == AccessibilityServiceEvent.Pong(key) } as AccessibilityServiceEvent.Pong?
                }

                if (pong == null) {
                    Timber.e("Service crashed so restarting")
                    disableServiceSuspend()
                    delay(200)
                    enableWithWriteSecureSettings()
                }
            }

            return true
        } else {
            Timber.i("Enable service by opening accessibility settings")

            return launchAccessibilitySettings()
        }
    }

    override fun restart(): Boolean {
        if (permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)) {
            Timber.i("Restarting service with WRITE_SECURE_SETTINGS")

            coroutineScope.launch {
                disableServiceSuspend()
                delay(200)
                enableWithWriteSecureSettings()
            }

            return true
        } else {
            Timber.i("Restarting service by opening accessibility settings")

            return launchAccessibilitySettings()
        }
    }

    override fun stop(): Boolean {
        coroutineScope.launch {
            disableServiceSuspend()
        }

        return true
    }

    private fun launchAccessibilitySettings(): Boolean {
        try {
            val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

            settingsIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    // Add this flag so user only has to press back once.
                    or Intent.FLAG_ACTIVITY_NO_HISTORY,
            )

            ctx.startActivity(settingsIntent)

            return true
        } catch (e: ActivityNotFoundException) {
            return false
        }
    }

    private suspend fun disableServiceSuspend() {
        // disableSelf method only exists in 7.0.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            send(AccessibilityServiceEvent.DisableService).onSuccess {
                Timber.i("Disabling service by calling disableSelf()")

                return
            }.onFailure {
                Timber.i("Failed to disable service by calling disableSelf()")
            }
        }

        if (permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)) {
            Timber.i("Disabling service with WRITE_SECURE_SETTINGS")

            val enabledServices = SettingsUtils.getSecureSetting<String>(
                ctx,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )

            enabledServices ?: return

            val className = classProvider.getAccessibilityService().name

            val keyMapperEntry = "${buildConfigProvider.packageName}/$className"

            val newEnabledServices = if (enabledServices.contains(keyMapperEntry)) {
                val services = enabledServices.split(':').toMutableList()
                services.remove(keyMapperEntry)

                services.joinToString(":")
            } else {
                enabledServices
            }

            SettingsUtils.putSecureSetting(
                ctx,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newEnabledServices,
            )

            return
        }

        launchAccessibilitySettings()
    }

    override suspend fun isCrashed(): Boolean {
        val key = "check_is_crashed"

        val pingJob = coroutineScope.launch {
            repeat(20) {
                eventsToService.emit(AccessibilityServiceEvent.Ping(key))
                delay(100)
            }
        }

        val pong: AccessibilityServiceEvent.Pong? = withTimeoutOrNull(2000L) {
            eventReceiver.first { it == AccessibilityServiceEvent.Pong(key) } as AccessibilityServiceEvent.Pong?
        }

        pingJob.cancel()

        if (pong == null) {
            Timber.e("Accessibility service is crashed")
        }

        return pong == null
    }

    override fun acknowledgeCrashed() {
        state.update { old ->
            if (old == AccessibilityServiceState.CRASHED) {
                AccessibilityServiceState.DISABLED
            } else {
                AccessibilityServiceState.ENABLED
            }
        }
    }

    override fun invalidateState() {
        coroutineScope.launch {
            state.value = getState()
        }
    }

    private fun enableWithWriteSecureSettings() {
        if (permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)) {
            Timber.i("Enable service with WRITE_SECURE_SETTINGS")

            val enabledServices = SettingsUtils.getSecureSetting<String>(
                ctx,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )

            val className = classProvider.getAccessibilityService().name

            val keyMapperEntry = "${buildConfigProvider.packageName}/$className"

            val newEnabledServices = when {
                enabledServices.isNullOrBlank() -> keyMapperEntry
                enabledServices.contains(keyMapperEntry) -> enabledServices
                else -> "$keyMapperEntry:$enabledServices"
            }

            SettingsUtils.putSecureSetting(
                ctx,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newEnabledServices,
            )

            SettingsUtils.putSecureSetting(
                ctx,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                1,
            )
        }
    }

    private suspend fun getState(): AccessibilityServiceState {
        /* get a list of all the enabled accessibility services.
         * The AccessibilityManager.getEnabledAccessibilityServices() method just returns an empty
         * list. :(*/
        val settingValue = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )

        if (settingValue == null) {
            return AccessibilityServiceState.DISABLED
        }

        // it can be null if the user has never interacted with accessibility settings before
        /* cant just use .contains because the debug and release accessibility service both contain
           io.github.sds100.keymapper. the enabled_accessibility_services are stored as

             io.github.sds100.keymapper.debug/io.github.sds100.keymapper.service.MyAccessibilityService
             :io.github.sds100.keymapper/io.github.sds100.keymapper.service.MyAccessibilityService

             without the new line before the :
         */
        val isEnabled = settingValue.split(':').any { it.split('/')[0] == ctx.packageName }

        return when {
            !isEnabled -> AccessibilityServiceState.DISABLED
            isCrashed() && isEnabled -> AccessibilityServiceState.CRASHED
            else -> AccessibilityServiceState.ENABLED
        }
    }
}
