package io.github.sds100.keymapper.system.apps

import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

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
    fun getInstallTime(packageName: String): Long
    fun getLastUpdateTime(packageName: String): Long

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

fun PackageManagerAdapter.isAppInstalledFlow(packageName: String): Flow<Boolean> = flow {
    emit(isAppInstalled(packageName))

    onPackagesChanged.collect {
        emit(isAppInstalled(packageName))
    }
}

fun PackageManagerAdapter.getPackageInfoFlow(packageName: String): Flow<PackageInfo?> = flow {
    emit(getPackageInfo(packageName))

    onPackagesChanged.collect {
        emit(getPackageInfo(packageName))
    }
}
