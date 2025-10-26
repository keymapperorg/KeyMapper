package io.github.sds100.keymapper.base.keymaps

import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import kotlinx.serialization.Serializable

@Serializable
data class TriggerKeyMapEvent(
    val uid: String,
) : AccessibilityServiceEvent()
