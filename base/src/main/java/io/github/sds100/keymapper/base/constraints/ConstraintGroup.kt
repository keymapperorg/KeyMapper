package io.github.sds100.keymapper.base.constraints

import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * @param mode how the constraints in this group are combined.
 */
@Serializable
data class ConstraintGroup(
    val uid: String = UUID.randomUUID().toString(),
    val name: String? = null,
    val constraints: List<Constraint> = emptyList(),
    val mode: ConstraintMode = ConstraintMode.AND,
)
