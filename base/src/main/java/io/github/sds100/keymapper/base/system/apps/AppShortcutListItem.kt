package io.github.sds100.keymapper.base.system.apps

import io.github.sds100.keymapper.base.utils.ui.IconInfo
import io.github.sds100.keymapper.base.utils.ui.SimpleListItemOld
import io.github.sds100.keymapper.base.utils.ui.TintType
import io.github.sds100.keymapper.system.apps.AppShortcutInfo

data class AppShortcutListItem(
    val shortcutInfo: AppShortcutInfo,
    val label: String,
    override val icon: IconInfo?,
) : SimpleListItemOld {
    override val id: String
        get() = shortcutInfo.toString()

    override val title: String
        get() = label

    override val subtitle: String? = null
    override val subtitleTint: TintType = TintType.None
    override val isEnabled: Boolean = true

    override fun getSearchableString() = label
}
