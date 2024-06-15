package io.github.sds100.keymapper.actions.uielementinteraction

import io.github.sds100.keymapper.system.ui.UiElementInfo
import kotlinx.serialization.Serializable

enum class InteractionType {
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
    val uiElement: UiElementInfo,
    val onlyIfVisible: Boolean,
    val interactionType: InteractionType,
    val description: String,
)
