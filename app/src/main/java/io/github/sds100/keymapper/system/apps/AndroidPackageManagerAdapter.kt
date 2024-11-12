package io.github.sds100.keymapper.system.apps

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.TransactionTooLargeException
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 16/03/2021.
 */
class AndroidPackageManagerAdapter(
    context: Context,
    coroutineScope: CoroutineScope,
) : PackageManagerAdapter {
    private val ctx: Context = context.applicationContext
    private val packageManager: PackageManager = ctx.packageManager

    override val installedPackages = MutableStateFlow<State<List<PackageInfo>>>(State.Loading)

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return
            intent ?: return

            when (intent.action) {
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED,
                -> {
                    coroutineScope.launch(Dispatchers.Default) {
                        updatePackageList()
                    }
                }
            }
        }
    }

    init {
        coroutineScope.launch(Dispatchers.Default) {
            // save memory by only storing this stuff as it is needed
            installedPackages.subscriptionCount
                .onEach { count ->
                    if (count == 0) {
                        installedPackages.value = State.Loading
                    }

                    if (count > 0 && installedPackages.value == State.Loading) {
                        updatePackageList()
                    }
                }
                .launchIn(this)
        }

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED)
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        filter.addDataScheme("package")

        ContextCompat.registerReceiver(
            ctx,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun downloadApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=$packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW)

            intent.data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            ctx.startActivity(intent)
        }
    }

    override fun launchVoiceAssistant(): Result<*> {
        try {
            Intent(Intent.ACTION_VOICE_COMMAND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(this)
            }

            return Success(Unit)
        } catch (e: ActivityNotFoundException) {
            return Error.NoVoiceAssistant
        }
    }

    override fun launchDeviceAssistant(): Result<*> {
        try {
            Intent(Intent.ACTION_ASSIST).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(this)
            }

            return Success(Unit)
        } catch (e: ActivityNotFoundException) {
            return Error.NoDeviceAssistant
        }
    }

    override fun getDeviceAssistantPackage(): Result<String> {
        try {
            val intent = Intent(Intent.ACTION_ASSIST)
            val activity =
                packageManager.resolveActivity(intent, 0) ?: return Error.NoDeviceAssistant

            return Success(activity.activityInfo!!.packageName)
        } catch (e: ActivityNotFoundException) {
            return Error.NoDeviceAssistant
        }
    }

    override fun enableApp(packageName: String) {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY.withFlag(Intent.FLAG_ACTIVITY_NEW_TASK)

            ctx.startActivity(this)
        }
    }

    override fun isAppEnabled(packageName: String): Result<Boolean> {
        when (val packagesState = installedPackages.value) {
            is State.Data -> {
                val packages = packagesState.data

                val appPackage = packages.find { it.packageName == packageName }

                if (appPackage == null) {
                    return Error.AppNotFound(packageName)
                } else {
                    return Success(appPackage.isEnabled)
                }
            }

            State.Loading -> return try {
                Success(packageManager.getApplicationInfo(packageName, 0).enabled)
            } catch (e: PackageManager.NameNotFoundException) {
                Error.AppNotFound(packageName)
            }
        }
    }

    override fun isAppInstalled(packageName: String): Boolean {
        when (val packagesState = installedPackages.value) {
            is State.Data -> {
                val packages = packagesState.data

                val appPackage = packages.find { it.packageName == packageName }

                return appPackage != null
            }

            State.Loading -> return try {
                packageManager.getApplicationInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag") // only specify the flag on SDK 23+. SDK 31 is first to enforce it.
    override fun openApp(packageName: String): Result<*> {
        val leanbackIntent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
        val normalIntent = packageManager.getLaunchIntentForPackage(packageName)

        val intent = leanbackIntent ?: normalIntent

        // intent = null if the app doesn't exist
        if (intent == null) {
            try {
                val appInfo = ctx.packageManager.getApplicationInfo(packageName, 0)

                // if the app is disabled, show an error message because it won't open
                if (!appInfo.enabled) {
                    return Error.AppDisabled(packageName)
                }

                return Success(Unit)
            } catch (e: Exception) {
                return Error.AppNotFound(packageName)
            }
        } else {
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getActivity(ctx, 0, intent, 0)
            }

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
        }
    }

    override fun isVoiceAssistantInstalled(): Boolean {
        val activityExists =
            Intent(Intent.ACTION_VOICE_COMMAND).resolveActivityInfo(ctx.packageManager, 0) != null

        return activityExists
    }

    override fun launchCameraApp(): Result<*> {
        try {
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(this)
            }

            return Success(Unit)
        } catch (e: ActivityNotFoundException) {
            return Error.NoCameraApp
        }
    }

    override fun launchSettingsApp(): Result<*> {
        try {
            Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(this)
            }

            return Success(Unit)
        } catch (e: ActivityNotFoundException) {
            return Error.NoSettingsApp
        }
    }

    override fun getAppName(packageName: String): Result<String> {
        try {
            return packageManager
                .getApplicationInfo(packageName, 0)
                .loadLabel(packageManager)
                .toString()
                .success()
        } catch (e: PackageManager.NameNotFoundException) {
            return Error.AppNotFound(packageName)
        }
    }

    override fun getAppIcon(packageName: String): Result<Drawable> {
        try {
            return packageManager
                .getApplicationInfo(packageName, 0)
                .loadIcon(packageManager)
                .success()
        } catch (e: PackageManager.NameNotFoundException) {
            return Error.AppNotFound(packageName)
        }
    }

    override fun getActivityLabel(packageName: String, activityClass: String): Result<String> {
        try {
            val component = ComponentName(packageName, activityClass)

            return packageManager
                .getActivityInfo(component, 0)
                .loadLabel(packageManager)
                .toString()
                .success()
        } catch (e: PackageManager.NameNotFoundException) {
            return Error.AppNotFound(packageName)
        }
    }

    override fun getActivityIcon(packageName: String, activityClass: String): Result<Drawable?> {
        try {
            val component = ComponentName(packageName, activityClass)

            return packageManager
                .getActivityInfo(component, 0)
                .loadIcon(packageManager)
                .success()
        } catch (e: PackageManager.NameNotFoundException) {
            return Error.AppNotFound(packageName)
        }
    }

    private suspend fun updatePackageList() {
        withContext(Dispatchers.Default) {
            installedPackages.value = State.Loading

            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .mapNotNull { createPackageInfoModel(it) }

            installedPackages.value = State.Data(packages)
        }
    }

    private fun createPackageInfoModel(applicationInfo: ApplicationInfo): PackageInfo? {
        val packageName = applicationInfo.packageName

        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            val leanbackLaunchIntent = packageManager.getLaunchIntentForPackage(packageName)

            val isLaunchable = launchIntent != null || leanbackLaunchIntent != null

            val activityPackageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES,
            )

            if (activityPackageInfo == null) {
                return null
            }

            val activityModels = mutableListOf<ActivityInfo>()

            activityPackageInfo.activities.forEach { activity ->
                val model = ActivityInfo(
                    activityName = activity.name,
                    packageName = activity.packageName,
                )

                activityModels.add(model)
            }

            return PackageInfo(
                packageName,
                activities = activityModels,
                isEnabled = applicationInfo.enabled,
                isLaunchable = isLaunchable,
            )
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        } catch (e: TransactionTooLargeException) {
            return null
        } catch (e: Exception) {
            return null
        }
    }
}
