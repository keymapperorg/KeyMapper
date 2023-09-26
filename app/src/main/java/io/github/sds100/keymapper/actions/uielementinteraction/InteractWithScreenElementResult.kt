package io.github.sds100.keymapper.actions.uielementinteraction

import kotlinx.serialization.Serializable

enum class INTERACTIONTYPE {
    CLICK,
    LONG_CLICK,
    SELECT,
    FOCUS,
    CLEAR_FOCUS,
    COLLAPSE,
    EXPAND,
    DISMISS,
    SCROLL_FORWARD,
    SCROLL_BACKWARD,
}

@Serializable
data class InteractWithScreenElementResult(
    val elementId: String,
    val packageName: String,
    val fullName: String,
    val appName: String?,
    val onlyIfVisible: Boolean,
    val interactionType: INTERACTIONTYPE,
    val description: String
)
