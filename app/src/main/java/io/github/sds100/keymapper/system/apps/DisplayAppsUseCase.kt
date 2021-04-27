package io.github.sds100.keymapper.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 04/04/2021.
 */

class DisplayAppsUseCaseImpl(
    private val adapter: PackageManagerAdapter
) : DisplayAppsUseCase {
    override val installedPackages: Flow<State<List<PackageInfo>>> = adapter.installedPackages

    override fun getAppName(packageName: String): Result<String> {
        return adapter.getAppName(packageName)
    }

    override fun getAppIcon(packageName: String): Result<Drawable> {
        return adapter.getAppIcon(packageName)
    }
}

interface DisplayAppsUseCase {
    val installedPackages: Flow<State<List<PackageInfo>>>

    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>
}