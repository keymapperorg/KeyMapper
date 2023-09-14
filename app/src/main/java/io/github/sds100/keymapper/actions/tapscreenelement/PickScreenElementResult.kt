package io.github.sds100.keymapper.actions.tapscreenelement

import kotlinx.serialization.Serializable

@Serializable
data class PickScreenElementResult(val elementId: String, val packageName: String, val fullName: String, val description: String)
