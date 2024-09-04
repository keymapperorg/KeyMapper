package io.github.sds100.keymapper.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 04/04/2021.
 */

class DisplayAppsUseCaseImpl(
    private val adapter: PackageManagerAdapter,
) : DisplayAppsUseCase {
    override val installedPackages: Flow<State<List<PackageInfo>>> = adapter.installedPackages

    override fun getAppName(packageName: String): Result<String> = adapter.getAppName(packageName)

    override fun getAppIcon(packageName: String): Result<Drawable> = adapter.getAppIcon(packageName)

    override fun getActivityLabel(packageName: String, activityClass: String): Result<String> =
        adapter.getActivityLabel(packageName, activityClass)

    override fun getActivityIcon(packageName: String, activityClass: String): Result<Drawable?> =
        adapter.getActivityIcon(packageName, activityClass)
}

interface DisplayAppsUseCase {
    val installedPackages: Flow<State<List<PackageInfo>>>

    fun getActivityLabel(packageName: String, activityClass: String): Result<String>
    fun getActivityIcon(packageName: String, activityClass: String): Result<Drawable?>
    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>
}
