package io.github.sds100.keymapper.system

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.system.accessibility.ObserveEnabledAccessibilityServicesJob
import io.github.sds100.keymapper.system.inputmethod.AndroidInputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.ObserveInputMethodsJob
import io.github.sds100.keymapper.system.notifications.ObserveNotificationListenersJob

/**
 * Created by sds100 on 02/04/2021.
 */
object JobSchedulerHelper {

    private const val ID_OBSERVE_ACCESSIBILITY_SERVICES = 1
    private const val ID_OBSERVE_ENABLED_INPUT_METHODS = 2
    private const val ID_OBSERVE_NOTIFICATION_LISTENERS = 3

    @RequiresApi(Build.VERSION_CODES.N)
    fun observeEnabledNotificationListeners(ctx: Context) {
        val uri = Settings.Secure.getUriFor("enabled_notification_listeners")

        val contentUri = JobInfo.TriggerContentUri(
            uri,
            JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
        )

        val builder = JobInfo.Builder(
            ID_OBSERVE_NOTIFICATION_LISTENERS,
            ComponentName(ctx, ObserveNotificationListenersJob::class.java),
        )
            .addTriggerContentUri(contentUri)
            .setTriggerContentUpdateDelay(500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setImportantWhileForeground(true)
        }

        ctx.getSystemService<JobScheduler>()?.schedule(builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun observeEnabledAccessibilityServices(ctx: Context) {
        val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

        val contentUri = JobInfo.TriggerContentUri(
            uri,
            JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
        )

        val builder = JobInfo.Builder(
            ID_OBSERVE_ACCESSIBILITY_SERVICES,
            ComponentName(ctx, ObserveEnabledAccessibilityServicesJob::class.java),
        )
            .addTriggerContentUri(contentUri)
            .setTriggerContentUpdateDelay(500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setImportantWhileForeground(true)
        }

        ctx.getSystemService<JobScheduler>()?.schedule(builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun observeInputMethods(ctx: Context) {
        val enabledContentUri = JobInfo.TriggerContentUri(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS),
            JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
        )

        val defaultImeContentUri = JobInfo.TriggerContentUri(
            Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),
            JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
        )

        val historyContentUri = JobInfo.TriggerContentUri(
            Settings.Secure.getUriFor(AndroidInputMethodAdapter.SETTINGS_SECURE_SUBTYPE_HISTORY_KEY),
            JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
        )

        val builder = JobInfo.Builder(
            ID_OBSERVE_ENABLED_INPUT_METHODS,
            ComponentName(ctx, ObserveInputMethodsJob::class.java),
        )
            .addTriggerContentUri(enabledContentUri)
            .addTriggerContentUri(defaultImeContentUri)
            .addTriggerContentUri(historyContentUri)
            .setTriggerContentUpdateDelay(500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setImportantWhileForeground(true)
        }

        ctx.getSystemService<JobScheduler>()?.schedule(builder.build())
    }
}
