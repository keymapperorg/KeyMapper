package io.github.sds100.keymapper.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Created by sds100 on 26/02/2021.
 */
interface PackageManagerAdapter {
    val onPackagesChanged: Flow<Unit>
    val installedPackages: StateFlow<State<List<PackageInfo>>>

    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>
    fun getPackageInfo(packageName: String): PackageInfo?
    fun getActivityLabel(packageName: String, activityClass: String): Result<String>
    fun getActivityIcon(packageName: String, activityClass: String): Result<Drawable?>
    fun isAppEnabled(packageName: String): Result<Boolean>
    fun isAppInstalled(packageName: String): Boolean

    fun openApp(packageName: String): Result<*>
    fun enableApp(packageName: String)
    fun downloadApp(packageName: String)

    fun launchVoiceAssistant(): Result<*>
    fun launchDeviceAssistant(): Result<*>
    fun isVoiceAssistantInstalled(): Boolean
    fun getDeviceAssistantPackage(): Result<String>

    fun launchCameraApp(): Result<*>
    fun launchSettingsApp(): Result<*>
}

fun PackageManagerAdapter.isAppInstalledFlow(packageName: String): Flow<Boolean> = callbackFlow {
    send(isAppInstalled(packageName))

    onPackagesChanged.collect {
        send(isAppInstalled(packageName))
    }
}

fun PackageManagerAdapter.getPackageInfoFlow(packageName: String): Flow<PackageInfo?> = callbackFlow {
    send(getPackageInfo(packageName))

    onPackagesChanged.collect {
        send(getPackageInfo(packageName))
    }
}
