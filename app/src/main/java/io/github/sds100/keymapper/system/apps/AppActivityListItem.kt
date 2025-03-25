package io.github.sds100.keymapper.system.apps

import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.SimpleListItemOld
import io.github.sds100.keymapper.util.ui.TintType

/**
 * Created by sds100 on 27/01/2020.
 */
data class AppActivityListItem(
    val appName: String,
    val activityInfo: ActivityInfo,
    override val icon: IconInfo?,
) : SimpleListItemOld {
    override val id: String
        get() = "${activityInfo.packageName}${activityInfo.activityName}"

    override val title: String
        get() = appName

    override val subtitle: String
        get() = activityInfo.activityName

    override val subtitleTint: TintType
        get() = TintType.None

    override val isEnabled: Boolean = true

    override fun getSearchableString() =
        "$appName ${activityInfo.packageName} ${activityInfo.activityName}"
}
