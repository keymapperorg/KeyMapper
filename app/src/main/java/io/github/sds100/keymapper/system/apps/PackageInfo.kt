package io.github.sds100.keymapper.system.apps

/**
 * Created by sds100 on 16/03/2021.
 */

data class PackageInfo(
    val packageName: String,

    /**
     * Whether the user can open this package like an "app".
     */
    val canBeLaunched: Boolean,
    val activities: List<ActivityInfo>
)
