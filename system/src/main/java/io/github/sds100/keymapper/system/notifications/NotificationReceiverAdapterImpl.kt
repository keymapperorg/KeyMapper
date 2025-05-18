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
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.withFlag
import io.github.sds100.keymapper.system.JobSchedulerHelper
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationReceiverAdapterImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val coroutineScope: CoroutineScope,
    private val buildConfigProvider: BuildConfigProvider,
) : NotificationReceiverAdapter {
    private val ctx: Context = context.applicationContext
    override val isEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val eventReceiver: MutableSharedFlow<NotificationServiceEvent> = MutableSharedFlow()
    val eventsToService = MutableSharedFlow<NotificationServiceEvent>()

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
                        isEnabled.update { getIsEnabled() }
                    }
                }
            }

            ctx.contentResolver.registerContentObserver(uri, false, observer)
        }

        coroutineScope.launch {
            isEnabled.update { getIsEnabled() }
        }
    }

    override suspend fun send(event: NotificationServiceEvent): Result<*> {
        if (isEnabled.value) {
            return SystemError.PermissionDenied(Permission.NOTIFICATION_LISTENER)
        }

        coroutineScope.launch {
            eventsToService.emit(event)
        }

        return Success(Unit)
    }

    override fun sendAsync(event: NotificationServiceEvent) {
        coroutineScope.launch {
            eventsToService.emit(event)
        }
    }

    override fun start(): Boolean = openSettingsPage()

    override fun restart(): Boolean = openSettingsPage()

    override fun stop(): Boolean = openSettingsPage()

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

    private fun getIsEnabled(): Boolean {
        val isEnabled = NotificationManagerCompat.getEnabledListenerPackages(ctx)
            .contains(buildConfigProvider.packageName)

        return isEnabled
    }
}
