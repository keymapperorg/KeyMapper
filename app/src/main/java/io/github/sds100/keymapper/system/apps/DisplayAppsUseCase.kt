package io.github.sds100.keymapper.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Created by sds100 on 04/04/2021.
 */

class DisplayAppsUseCaseImpl @Inject constructor(
    private val adapter: PackageManagerAdapter
) : DisplayAppsUseCase {
    override val installedPackages: Flow<State<List<PackageInfo>>> = adapter.installedPackages

    override fun getAppName(packageName: String): Result<String> {
        return adapter.getAppName(packageName)
    }

    override fun getAppIcon(packageName: String): Result<Drawable> {
        return adapter.getAppIcon(packageName)
    }

    override fun getActivityLabel(packageName: String, activityClass: String): Result<String> {
        return adapter.getActivityLabel(packageName, activityClass)
    }

    override fun getActivityIcon(packageName: String, activityClass: String): Result<Drawable?> {
        return adapter.getActivityIcon(packageName, activityClass)
    }
}

interface DisplayAppsUseCase {
    val installedPackages: Flow<State<List<PackageInfo>>>

    fun getActivityLabel(packageName: String, activityClass: String): Result<String>
    fun getActivityIcon(packageName: String, activityClass: String): Result<Drawable?>
    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>
}