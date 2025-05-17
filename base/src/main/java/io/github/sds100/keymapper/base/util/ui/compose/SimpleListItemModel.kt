package io.github.sds100.keymapper.base.util.ui.compose

data class SimpleListItemGroup(val header: String, val items: List<SimpleListItemModel>)

data class SimpleListItemModel(
    val id: String,
    val title: String,
    val icon: ComposeIconInfo,
    val subtitle: String? = null,
    val isSubtitleError: Boolean = false,
    val isEnabled: Boolean = true,
)
