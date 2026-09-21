package io.github.sds100.keymapper.base.constraints

import kotlinx.serialization.Serializable

/**
 * How the text the user entered is compared against the value of a notification field.
 */
@Serializable
enum class TextMatchMode {
    CONTAINS,
    EXACT,
}
