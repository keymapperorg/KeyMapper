package io.github.sds100.keymapper.base.system.apps

import io.github.sds100.keymapper.base.utils.ui.IconInfo
import io.github.sds100.keymapper.base.utils.ui.SimpleListItemOld
import io.github.sds100.keymapper.base.utils.ui.TintType
import io.github.sds100.keymapper.system.apps.ActivityInfo

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
