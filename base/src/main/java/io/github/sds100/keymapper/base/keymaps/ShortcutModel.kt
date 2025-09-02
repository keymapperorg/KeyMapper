package io.github.sds100.keymapper.base.keymaps

import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo

data class ShortcutModel<T>(
    val icon: ComposeIconInfo,
    val text: String,
    val data: T,
)
