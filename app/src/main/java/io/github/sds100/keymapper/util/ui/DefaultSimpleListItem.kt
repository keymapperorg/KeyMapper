package io.github.sds100.keymapper.util.ui

/**
 * Created by sds100 on 22/07/2021.
 */
data class DefaultSimpleListItem(
    override val id: String,
    override val title: String,
    override val subtitle: String? = null,
    override val subtitleTint: TintType = TintType.OnSurface,
    override val icon: IconInfo? = null,
    override val isEnabled: Boolean = true,
) : SimpleListItem {
    override fun getSearchableString(): String = title
}

interface SimpleListItem :
    ListItem,
    ISearchable {
    override val id: String
    val title: String
    val subtitle: String?
    val subtitleTint: TintType
    val icon: IconInfo?
    val isEnabled: Boolean
}
