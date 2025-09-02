package io.github.sds100.keymapper.system.intents

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class IntentExtraModel(
    val type: IntentExtraType,
    val name: String = "",
    val value: String = "",
    val uid: String = UUID.randomUUID().toString(),
) {
    val isValidValue: Boolean
        get() = type.isValid(value)

    val parsedValue: Any?
        get() = type.parse(value)
}
