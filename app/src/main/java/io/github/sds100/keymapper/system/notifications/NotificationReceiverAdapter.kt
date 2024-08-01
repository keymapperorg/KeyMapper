package io.github.sds100.keymapper.system.notifications

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.system.JobSchedulerHelper
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.ServiceEvent
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 27/07/2021.
 */

class NotificationReceiverAdapter(
    context: Context,
    private val coroutineScope: CoroutineScope,
) : ServiceAdapter {
    private val ctx: Context = context.applicationContext
    override val state: MutableStateFlow<ServiceState> = MutableStateFlow(ServiceState.DISABLED)

    override val eventReceiver: MutableSharedFlow<ServiceEvent> = MutableSharedFlow()
    val eventsToService = MutableSharedFlow<ServiceEvent>()

    init {
        // use job scheduler because there is there is a much shorter delay when the app is in the background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            JobSchedulerHelper.observeEnabledNotificationListeners(ctx)
        } else {
            val uri = Settings.Secure.getUriFor("enabled_notification_listeners")
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
    }

    override suspend fun send(event: ServiceEvent): Result<*> {
        if (state.value != ServiceState.ENABLED) {
            return Error.PermissionDenied(Permission.NOTIFICATION_LISTENER)
        }

        coroutineScope.launch {
            eventsToService.emit(event)
        }

        return Success(Unit)
    }

    override fun start(): Boolean = openSettingsPage()

    override fun restart(): Boolean = openSettingsPage()

    override fun stop(): Boolean = openSettingsPage()

    override suspend fun isCrashed(): Boolean = false

    private fun openSettingsPage(): Boolean {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                .withFlag(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .withFlag(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

            try {
                ctx.startActivity(this)
                return true
            } catch (e: ActivityNotFoundException) {
                return false
            }
        }
    }

    private fun getState(): ServiceState {
        val isEnabled = NotificationManagerCompat.getEnabledListenerPackages(ctx)
            .contains(Constants.PACKAGE_NAME)

        return if (isEnabled) {
            ServiceState.ENABLED
        } else {
            ServiceState.DISABLED
        }
    }
}
