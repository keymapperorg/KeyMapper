package io.github.sds100.keymapper.actions.tapscreenelement

import kotlinx.serialization.Serializable

enum class INTERACTION_TYPES {
    CLICK,
    LONG_CLICK
}
@Serializable
data class PickScreenElementResult(val elementId: String, val packageName: String, val fullName: String, val onlyIfVisible: Boolean, val description: String)
