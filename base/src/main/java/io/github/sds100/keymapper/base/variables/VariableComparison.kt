package io.github.sds100.keymapper.base.variables

import kotlinx.serialization.Serializable

@Serializable
enum class VariableComparison {
    EQUALS,
    GREATER_THAN,
    LESS_THAN,
    ;

    fun next(): VariableComparison {
        return entries[(ordinal + 1) % entries.size]
    }
}
