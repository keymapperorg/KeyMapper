package io.github.sds100.keymapper.common.ui

data class DefaultSimpleListItem(
    override val id: String,
    override val title: String,
    override val subtitle: String? = null,
    override val subtitleTint: TintType = TintType.OnSurface,
    override val icon: IconInfo? = null,
    override val isEnabled: Boolean = true,
) : SimpleListItemOld {
    override fun getSearchableString(): String = title
}

interface SimpleListItemOld :
    ListItem,
    ISearchable {
    override val id: String
    val title: String
    val subtitle: String?
    val subtitleTint: TintType
    val icon: IconInfo?
    val isEnabled: Boolean
}
