package io.github.sds100.keymapper.system.apps

import android.app.ActivityOptions
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
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppShortcutAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyMapShortcutActivityIntentBuilder: KeyMapShortcutActivityIntentBuilder,
) : AppShortcutAdapter {
    private val ctx = context.applicationContext

    override val installedAppShortcuts: Flow<State<List<AppShortcutInfo>>> = flow {
        emit(State.Loading)

        val shortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)

        val shortcuts = ctx.packageManager.queryIntentActivities(shortcutIntent, 0)
            .map {
                val activityInfo = it.activityInfo
                AppShortcutInfo(
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name,
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
        intentExtras: Bundle,
    ): ShortcutInfoCompat {
        return createLauncherShortcut(
            IconCompat.createWithBitmap(icon.toBitmap()),
            label,
            intentAction,
            intentExtras,
        )
    }

    override fun createLauncherShortcut(
        iconResId: Int,
        label: String,
        intentAction: String,
        intentExtras: Bundle,
    ): ShortcutInfoCompat {
        return createLauncherShortcut(
            IconCompat.createWithResource(ctx, iconResId),
            label,
            intentAction,
            intentExtras,
        )
    }

    private fun createLauncherShortcut(
        icon: IconCompat,
        label: String,
        intentAction: String,
        intentExtras: Bundle,
    ): ShortcutInfoCompat {
        val builder = ShortcutInfoCompat.Builder(ctx, UUID.randomUUID().toString()).apply {
            setIcon(icon)
            setShortLabel(label)
            val intent = keyMapShortcutActivityIntentBuilder.build(intentAction, intentExtras)
            setIntent(intent)
        }

        return builder.build()
    }

    override fun pinShortcut(shortcut: ShortcutInfoCompat): KMResult<*> {
        val supported = ShortcutManagerCompat.requestPinShortcut(ctx, shortcut, null)

        if (!supported) {
            return KMError.LauncherShortcutsNotSupported
        } else {
            return Success(Unit)
        }
    }

    override fun createShortcutResultIntent(shortcut: ShortcutInfoCompat): Intent = ShortcutManagerCompat.createShortcutResultIntent(ctx, shortcut)

    override fun getShortcutName(info: AppShortcutInfo): KMResult<String> {
        try {
            return ctx.packageManager
                .getActivityInfo(ComponentName(info.packageName, info.activityName), 0)
                .loadLabel(ctx.packageManager)
                .toString()
                .success()
        } catch (e: PackageManager.NameNotFoundException) {
            return KMError.AppNotFound(info.packageName)
        }
    }

    override fun getShortcutIcon(info: AppShortcutInfo): KMResult<Drawable> {
        try {
            return ctx.packageManager
                .getActivityInfo(ComponentName(info.packageName, info.activityName), 0)
                .loadIcon(ctx.packageManager)
                .success()
        } catch (e: PackageManager.NameNotFoundException) {
            return KMError.AppNotFound(info.packageName)
        }
    }

    override fun launchShortcut(uri: String): KMResult<*> {
        val intent = Intent.parseUri(uri, 0)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            // See issue #1222 and #1307. Must have FLAG_UPDATE_CURRENT so that
            // the intent data is updated. If you don't do this and have two app shortcut actions
            // from the same app then the data isn't updated and both actions will send
            // the pending intent for the shortcut that was triggered first.
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = PendingIntent.getActivity(ctx, 0, intent, flags)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val bundle = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                    )
                    .toBundle()

                pendingIntent.send(bundle)
            } else {
                pendingIntent.send()
            }

            return Success(Unit)
        } catch (e: SecurityException) {
            return KMError.InsufficientPermissionsToOpenAppShortcut
        } catch (e: Exception) {
            return KMError.AppShortcutCantBeOpened
        }
    }
}
