package io.github.sds100.keymapper.system.ui

import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.SimpleListItem
import io.github.sds100.keymapper.util.ui.TintType

class UiElementInfoListItem (
    override val id: String,
    override val title: String,
    override val subtitle: String?,
    override val subtitleTint: TintType = TintType.None,
    override val icon: IconInfo?,
    override val isEnabled: Boolean = true
): SimpleListItem {
    override fun getSearchableString(): String {
        return id
    }
}