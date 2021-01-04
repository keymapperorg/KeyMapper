package io.github.sds100.keymapper.data.model

import java.util.*

/**
 * Created by sds100 on 01/01/21.
 */
data class IntentExtraModel(
    val type: IntentExtraType,
    val name: String = "",
    val value: String = "",
    val uid: String = UUID.randomUUID().toString()
) {
    val isValid: Boolean
        get() = type.isValid(value)
}