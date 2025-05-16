package io.github.sds100.keymapper.system.intents

import kotlinx.serialization.Serializable

@Serializable
data class ConfigIntentResult(
    val uri: String,
    val target: IntentTarget,
    val description: String,
    val extras: List<IntentExtraModel>,
)
