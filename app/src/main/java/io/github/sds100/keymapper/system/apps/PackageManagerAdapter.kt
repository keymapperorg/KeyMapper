package io.github.sds100.keymapper.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 26/02/2021.
 */
interface PackageManagerAdapter {
    val installedPackages: StateFlow<State<List<PackageInfo>>>

    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>
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

    fun launchCameraApp(): Result<*>
    fun launchSettingsApp(): Result<*>
}