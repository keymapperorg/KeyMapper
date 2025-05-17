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
import android.os.BadParcelableException
import android.os.Build
import android.os.RemoteException
import android.os.TransactionTooLargeException
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import io.github.sds100.keymapper.common.util.result.Error
import io.github.sds100.keymapper.common.util.result.Result
import io.github.sds100.keymapper.common.util.result.Success
import io.github.sds100.keymapper.common.util.result.success
import io.github.sds100.keymapper.common.util.state.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class AndroidPackageManagerAdapter(
    context: Context,
    coroutineScope: CoroutineScope,
) : PackageManagerAdapter {
    private val ctx: Context = context.applicationContext
    private val packageManager: PackageManager = ctx.packageManager

    private val fetchPackages: MutableSharedFlow<Unit> = MutableSharedFlow()
    override val onPackagesChanged: MutableSharedFlow<Unit> = MutableSharedFlow()
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
                    coroutineScope.launch {
                        fetchPackages.emit(Unit)
                        onPackagesChanged.emit(Unit)
                    }
                }
            }
        }
    }

    init {
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

        coroutineScope.launch {
            // Collect latest so packages aren't being fetched multiple times at the same time.
            fetchPackages.collectLatest {
                if (installedPackages.subscriptionCount.value == 0) {
                    return@collectLatest
                }
                installedPackages.value = State.Loading

                val packages = withContext(Dispatchers.Default) {
                    try {
                        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                            .mapNotNull { createPackageInfoModel(it) }
                    } catch (_: BadParcelableException) {
                        emptyList()
                    } catch (_: RemoteException) {
                        emptyList()
                    }
                }

                installedPackages.value = State.Data(packages)
            }
        }

        coroutineScope.launch {

            // save memory by only storing this stuff as it is needed
            installedPackages.subscriptionCount
                .map { count -> count > 0 }
                .distinctUntilChanged().collect { isActive ->
                    if (isActive) {
                        fetchPackages.emit(Unit)
                    } else {
                        installedPackages.value = State.Loading
                    }
                }
        }
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
        val settingValue = Settings.Secure.getString(ctx.contentResolver, "assistant")

        if (settingValue.isNullOrEmpty()) {
            return Error.NoDeviceAssistant
        }

        val packageName = settingValue.split("/").first()
        return Success(packageName)
    }

    override fun enableApp(packageName: String) {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK

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
            /**
             * See this guide on how the camera is launched with double press power button in
             * SystemUI. https://cs.android.com/android/platform/superproject/+/master:frameworks/base/packages/SystemUI/docs/camera.md
             *
             * First launch the SECURE camera intent so the camera opens when the device
             * is locked.
             */
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(this)
            }

            return Success(Unit)
        } catch (e: ActivityNotFoundException) {
            // Just in case the camera app didn't implement the secure intent, try the normal one.
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

    override fun getPackageInfo(packageName: String): PackageInfo? {
        try {
            val applicationInfo =
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

            return createPackageInfoModel(applicationInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
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
        } catch (e: IOException) {
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
        } catch (e: IOException) {
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

    private fun createPackageInfoModel(applicationInfo: ApplicationInfo): PackageInfo? {
        val packageName = applicationInfo.packageName

        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            val leanbackLaunchIntent = packageManager.getLaunchIntentForPackage(packageName)

            val isLaunchable = launchIntent != null || leanbackLaunchIntent != null

            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA,
            )

            if (packageInfo == null) {
                return null
            }

            val activityModels = mutableListOf<ActivityInfo>()

            packageInfo.activities?.forEach { activity ->
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
                versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
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
