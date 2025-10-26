package io.github.sds100.keymapper.base.groups

import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo

data class GroupListItemModel(
    val uid: String,
    val name: String,
    val icon: ComposeIconInfo? = null,
)
