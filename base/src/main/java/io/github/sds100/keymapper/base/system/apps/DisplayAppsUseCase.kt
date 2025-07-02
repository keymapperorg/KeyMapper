package io.github.sds100.keymapper.base.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.apps.PackageInfo
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DisplayAppsUseCaseImpl @Inject constructor(
    private val adapter: PackageManagerAdapter,
) : DisplayAppsUseCase {
    override val installedPackages: Flow<State<List<PackageInfo>>> = adapter.installedPackages

    override fun getAppName(packageName: String): KMResult<String> = adapter.getAppName(packageName)

    override fun getAppIcon(packageName: String): KMResult<Drawable> = adapter.getAppIcon(packageName)

    override fun getActivityLabel(packageName: String, activityClass: String): KMResult<String> = adapter.getActivityLabel(packageName, activityClass)

    override fun getActivityIcon(packageName: String, activityClass: String): KMResult<Drawable?> = adapter.getActivityIcon(packageName, activityClass)
}

interface DisplayAppsUseCase {
    val installedPackages: Flow<State<List<PackageInfo>>>

    fun getActivityLabel(packageName: String, activityClass: String): KMResult<String>
    fun getActivityIcon(packageName: String, activityClass: String): KMResult<Drawable?>
    fun getAppName(packageName: String): KMResult<String>
    fun getAppIcon(packageName: String): KMResult<Drawable>
}
