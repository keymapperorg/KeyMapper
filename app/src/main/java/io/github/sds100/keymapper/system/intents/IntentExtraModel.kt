package io.github.sds100.keymapper.system.intents

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Created by sds100 on 01/01/21.
 */
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
