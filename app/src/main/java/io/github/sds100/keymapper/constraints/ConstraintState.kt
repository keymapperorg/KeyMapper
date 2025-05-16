package io.github.sds100.keymapper.constraints

import kotlinx.serialization.Serializable

@Serializable
data class ConstraintState(
    val constraints: Set<Constraint> = emptySet(),
    val mode: ConstraintMode = ConstraintMode.AND,
)
