package io.github.sds100.keymapper.system.apps

import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 04/04/2021.
 */
@Serializable
data class ChooseAppShortcutResult(
    val packageName: String?,
    val shortcutName: String,
    val uri: String,
)
