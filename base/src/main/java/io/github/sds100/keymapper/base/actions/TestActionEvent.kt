package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import kotlinx.serialization.Serializable

@Serializable
data class TestActionEvent(val action: ActionData) : AccessibilityServiceEvent()
