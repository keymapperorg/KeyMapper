package io.github.sds100.keymapper.system.apps

import kotlinx.serialization.Serializable

@Serializable
data class ChooseAppShortcutResult(
    val packageName: String?,
    val shortcutName: String,
    val uri: String,
)
