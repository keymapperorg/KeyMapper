package io.github.sds100.keymapper.base.ui.intents

import io.github.sds100.keymapper.system.intents.IntentExtraModel
import io.github.sds100.keymapper.system.intents.IntentTarget
import kotlinx.serialization.Serializable

@Serializable
data class ConfigIntentResult(
    val uri: String,
    val target: IntentTarget,
    val description: String,
    val extras: List<IntentExtraModel>,
)
