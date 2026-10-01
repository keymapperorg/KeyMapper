package io.github.sds100.keymapper.base.variables

import kotlinx.serialization.Serializable

@Serializable
enum class VariableOperation {
    ADD,
    SUBTRACT,
    SET,
    ;

    fun next(): VariableOperation {
        return entries[(ordinal + 1) % entries.size]
    }
}
