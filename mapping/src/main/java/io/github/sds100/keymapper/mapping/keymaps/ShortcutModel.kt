package io.github.sds100.keymapper.mapping.keymaps

import io.github.sds100.keymapper.base.util.ui.compose.ComposeIconInfo

data class ShortcutModel<T>(
    val icon: ComposeIconInfo,
    val text: String,
    val data: T,
)
