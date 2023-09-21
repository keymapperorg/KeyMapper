package io.github.sds100.keymapper.actions.uielementinteraction

import kotlinx.serialization.Serializable

enum class INTERACTIONTYPE {
    CLICK,
    LONG_CLICK
}

@Serializable
data class InteractWithScreenElementResult(
    val elementId: String,
    val packageName: String,
    val fullName: String,
    val onlyIfVisible: Boolean,
    val description: String
)
