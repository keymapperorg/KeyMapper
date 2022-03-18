package io.github.sds100.keymapper.system.apps

import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.SimpleListItem
import io.github.sds100.keymapper.util.ui.TintType

/**
 * Created by sds100 on 29/03/2020.
 */

data class AppShortcutListItem(
    val shortcutInfo: AppShortcutInfo,
    val label: String,
    override val icon: IconInfo?
) : SimpleListItem {
    override val id: String
        get() = shortcutInfo.toString()

    override val title: String
        get() = label

    override val subtitle: String? = null
    override val subtitleTint: TintType = TintType.None
    override val isEnabled: Boolean = true


    override fun getSearchableString() = label
}