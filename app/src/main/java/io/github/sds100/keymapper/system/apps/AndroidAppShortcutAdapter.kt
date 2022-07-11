package io.github.sds100.keymapper.system.apps

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.api.LaunchKeyMapShortcutActivity
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by sds100 on 20/03/2021.
 */
@Singleton
class AndroidAppShortcutAdapter @Inject constructor(@ApplicationContext context: Context) : AppShortcutAdapter {
    private val ctx = context.applicationContext

    override val installedAppShortcuts: Flow<State<List<AppShortcutInfo>>> = flow {
        emit(State.Loading)

        val shortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)

        val shortcuts = ctx.packageManager.queryIntentActivities(shortcutIntent, 0)
            .map {
                val activityInfo = it.activityInfo
                AppShortcutInfo(
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name
                )
            }

        emit(State.Data(shortcuts))
    }

    override val areLauncherShortcutsSupported: Boolean
        get() = ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)

    override fun createLauncherShortcut(
        icon: Drawable,
        label: String,
        intentAction: String,
        intentExtras: Bundle
    ): ShortcutInfoCompat {
        val builder = ShortcutInfoCompat.Builder(ctx, UUID.randomUUID().toString()).apply {
            setIcon(IconCompat.createWithBitmap(icon.toBitmap()))
            setShortLabel(label)

            Intent(ctx, LaunchKeyMapShortcutActivity::class.java).apply {
                action = intentAction

                putExtras(intentExtras)

                setIntent(this)
            }
        }

        return builder.build()
    }

    override fun pinShortcut(shortcut: ShortcutInfoCompat): Result<*> {
        val supported = ShortcutManagerCompat.requestPinShortcut(ctx, shortcut, null)

        if (!supported) {
            return Error.LauncherShortcutsNotSupported
        } else {
            return Success(Unit)
        }
    }

    override fun createShortcutResultIntent(shortcut: ShortcutInfoCompat): Intent {
        return ShortcutManagerCompat.createShortcutResultIntent(ctx, shortcut)
    }

    override fun getShortcutName(info: AppShortcutInfo): Result<String> {
        try {
            return ctx.packageManager
                .getActivityInfo(ComponentName(info.packageName, info.activityName), 0)
                .loadLabel(ctx.packageManager)
                .toString()
                .success()
        } catch (e: PackageManager.NameNotFoundException) {
            return Error.AppNotFound(info.packageName)
        }
    }

    override fun getShortcutIcon(info: AppShortcutInfo): Result<Drawable> {
        try {
            return ctx.packageManager
                .getActivityInfo(ComponentName(info.packageName, info.activityName), 0)
                .loadIcon(ctx.packageManager)
                .success()
        } catch (e: PackageManager.NameNotFoundException) {
            return Error.AppNotFound(info.packageName)
        }
    }

    override fun launchShortcut(uri: String): Result<*> {
        val intent = Intent.parseUri(uri, 0)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getActivity(ctx, 0, intent, 0)
            }

            pendingIntent.send()
            return Success(Unit)
        } catch (e: SecurityException) {
            return Error.InsufficientPermissionsToOpenAppShortcut
        } catch (e: Exception) {
            return Error.AppShortcutCantBeOpened
        }
    }
}