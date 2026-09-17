package io.github.sds100.keymapper.base.constraints

import kotlinx.serialization.Serializable

/**
 * @param mode how the results of each group are combined.
 */
@Serializable
data class ConstraintState(
    val groups: List<ConstraintGroup> = emptyList(),
    val mode: ConstraintMode = ConstraintMode.AND,
) {
    val constraints: List<Constraint>
        get() = groups.flatMap { it.constraints }
}
