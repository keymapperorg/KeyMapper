package io.github.sds100.keymapper.constraints

import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 04/04/2021.
 */

@Serializable
data class ConstraintState(
    val constraints: Set<Constraint> = emptySet(),
    val mode: ConstraintMode = ConstraintMode.AND,
)
