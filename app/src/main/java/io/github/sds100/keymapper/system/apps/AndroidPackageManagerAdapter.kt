package io.github.sds100.keymapper.system.apps

import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 16/03/2021.
 */
class AndroidPackageManagerAdapter(
    context: Context,
    coroutineScope: CoroutineScope
) : PackageManagerAdapter {
    private val ctx = context.applicationContext
    private val packageManager = ctx.packageManager

    override val installedPackages = MutableStateFlow<State<List<PackageInfo>>>(State.Loading)

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return
            intent ?: return

            when (intent.action) {
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED -> {
                    coroutineScope.launch(Dispatchers.Default) {
                        updatePackageList()
                    }
                }
            }
        }
    }

    init {
        coroutineScope.launch(Dispatchers.Default) {
            updatePackageList()
        }

        IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")

            ctx.registerReceiver(broadcastReceiver, this)
        }
    }

    override fun installApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=$packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ctx.startActivity(intent)

        } catch (e: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
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

    override fun enableApp(packageName: String) {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${packageName}")
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY.withFlag(Intent.FLAG_ACTIVITY_NEW_TASK)

            ctx.startActivity(this)
        }
    }

    override fun isAppEnabled(packageName: String): Result<Boolean> {
        return try {
            Success(packageManager.getApplicationInfo(packageName, 0).enabled)
        } catch (e: PackageManager.NameNotFoundException) {
            Error.AppNotFound(packageName)
        }
    }

    override fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun openApp(packageName: String): Result<*> {
        val leanbackIntent = packageManager.getLeanbackLaunchIntentForPackage(packageName)

        val normalIntent = packageManager.getLaunchIntentForPackage(packageName)

        val intent = leanbackIntent ?: normalIntent

        //intent = null if the app doesn't exist
        if (intent != null) {
            ctx.startActivity(intent)
            return Success(Unit)

        } else {
            try {
                val appInfo = ctx.packageManager.getApplicationInfo(packageName, 0)

                //if the app is disabled, show an error message because it won't open
                if (!appInfo.enabled) {
                    return Error.AppDisabled(packageName)
                }

                return Success(Unit)

            } catch (e: Exception) {
                return Error.AppNotFound(packageName)
            }
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

    private fun updatePackageList() {
        installedPackages.value = State.Loading

        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { applicationInfo ->
                val packageName = applicationInfo.packageName
                val canBeLaunched =
                    (packageManager.getLaunchIntentForPackage(packageName) != null
                        || packageManager.getLeanbackLaunchIntentForPackage(packageName) != null)

                val activities =
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.GET_ACTIVITIES
                    )?.activities?.map {
                        ActivityInfo(it.name, it.packageName)
                    }

                PackageInfo(
                    packageName,
                    canBeLaunched,
                    activities = activities ?: emptyList()
                )
            }

        installedPackages.value = State.Data(packages)
    }
}