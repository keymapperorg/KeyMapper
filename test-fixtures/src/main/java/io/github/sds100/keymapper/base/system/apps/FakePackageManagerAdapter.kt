package io.github.sds100.keymapper.base.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.system.apps.PackageInfo
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePackageManagerAdapter : PackageManagerAdapter {
    override val onPackagesChanged = MutableSharedFlow<Unit>()

    override val installedPackages =
        MutableStateFlow<State<List<PackageInfo>>>(State.Data(emptyList()))

    /**
     * The package names passed to [openApp], in order.
     */
    val openedApps = mutableListOf<String>()

    private val packages: List<PackageInfo>
        get() = installedPackages.value.dataOrNull().orEmpty()

    suspend fun installPackage(packageInfo: PackageInfo) {
        installedPackages.value = State.Data(packages + packageInfo)
        onPackagesChanged.emit(Unit)
    }

    suspend fun uninstallPackage(packageName: String) {
        installedPackages.value = State.Data(packages.filter { it.packageName != packageName })
        onPackagesChanged.emit(Unit)
    }

    override fun getPackageInfo(packageName: String): PackageInfo? {
        return packages.find { it.packageName == packageName }
    }

    override fun isAppInstalled(packageName: String): Boolean {
        return getPackageInfo(packageName) != null
    }

    override fun isAppEnabled(packageName: String): KMResult<Boolean> {
        val packageInfo = getPackageInfo(packageName) ?: return KMError.AppNotFound(packageName)
        return Success(packageInfo.isEnabled)
    }

    override fun openApp(packageName: String): KMResult<*> {
        val packageInfo = getPackageInfo(packageName) ?: return KMError.AppNotFound(packageName)

        if (!packageInfo.isEnabled) {
            return KMError.AppDisabled(packageName)
        }

        openedApps.add(packageName)
        return Success(Unit)
    }

    override fun getAppName(packageName: String): KMResult<String> {
        throw NotImplementedError()
    }

    override fun getAppIcon(packageName: String): KMResult<Drawable> {
        throw NotImplementedError()
    }

    override fun getActivityLabel(packageName: String, activityClass: String): KMResult<String> {
        throw NotImplementedError()
    }

    override fun getActivityIcon(packageName: String, activityClass: String): KMResult<Drawable?> {
        throw NotImplementedError()
    }

    override fun getInstallTime(packageName: String): Long {
        throw NotImplementedError()
    }

    override fun getLastUpdateTime(packageName: String): Long {
        throw NotImplementedError()
    }

    override fun enableApp(packageName: String) {
        throw NotImplementedError()
    }

    override fun downloadApp(packageName: String) {
        throw NotImplementedError()
    }

    override fun launchVoiceAssistant(): KMResult<*> {
        throw NotImplementedError()
    }

    override fun launchDeviceAssistant(): KMResult<*> {
        throw NotImplementedError()
    }

    override fun isVoiceAssistantInstalled(): Boolean {
        throw NotImplementedError()
    }

    override fun getDeviceAssistantPackage(): KMResult<String> {
        throw NotImplementedError()
    }

    override fun launchCameraApp(): KMResult<*> {
        throw NotImplementedError()
    }

    override fun launchSettingsApp(): KMResult<*> {
        throw NotImplementedError()
    }

    override fun getInstallSourcePackageName(): String? {
        throw NotImplementedError()
    }
}
