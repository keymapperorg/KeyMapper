package io.github.sds100.keymapper.system.intents

import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 20/04/2021.
 */

@Serializable
data class ConfigIntentResult(
    val uri: String,
    val target: IntentTarget,
    val description: String,
    val extras: List<IntentExtraModel>,
)
