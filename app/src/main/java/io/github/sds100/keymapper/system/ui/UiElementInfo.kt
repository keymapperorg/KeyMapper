package io.github.sds100.keymapper.system.ui

import kotlinx.serialization.Serializable

@Serializable
data class UiElementInfo(
    val elementName: String,
    val packageName: String,
    val fullName: String,
)
