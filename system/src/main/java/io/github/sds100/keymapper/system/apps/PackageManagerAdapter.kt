package io.github.sds100.keymapper.system.apps

import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

interface PackageManagerAdapter {
    val onPackagesChanged: Flow<Unit>
    val installedPackages: StateFlow<State<List<PackageInfo>>>

    fun getAppName(packageName: String): KMResult<String>
    fun getAppIcon(packageName: String): KMResult<Drawable>
    fun getPackageInfo(packageName: String): PackageInfo?
    fun getActivityLabel(packageName: String, activityClass: String): KMResult<String>
    fun getActivityIcon(packageName: String, activityClass: String): KMResult<Drawable?>
    fun isAppEnabled(packageName: String): KMResult<Boolean>
    fun isAppInstalled(packageName: String): Boolean

    fun openApp(packageName: String): KMResult<*>
    fun enableApp(packageName: String)
    fun downloadApp(packageName: String)

    fun launchVoiceAssistant(): KMResult<*>
    fun launchDeviceAssistant(): KMResult<*>
    fun isVoiceAssistantInstalled(): Boolean
    fun getDeviceAssistantPackage(): KMResult<String>

    fun launchCameraApp(): KMResult<*>
    fun launchSettingsApp(): KMResult<*>

    @RequiresApi(Build.VERSION_CODES.R)
    fun getInstallSourcePackageName(): String?
}

fun PackageManagerAdapter.isAppInstalledFlow(packageName: String): Flow<Boolean> = callbackFlow {
    send(isAppInstalled(packageName))

    onPackagesChanged.collect {
        send(isAppInstalled(packageName))
    }
}

fun PackageManagerAdapter.getPackageInfoFlow(packageName: String): Flow<PackageInfo?> =
    callbackFlow {
        send(getPackageInfo(packageName))

        onPackagesChanged.collect {
            send(getPackageInfo(packageName))
        }
    }
