package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.data.entities.ConstraintEntity

object ConstraintModeEntityMapper {
    fun fromEntity(entity: Int): ConstraintMode = when (entity) {
        ConstraintEntity.MODE_AND -> ConstraintMode.AND
        ConstraintEntity.MODE_OR -> ConstraintMode.OR
        else -> throw Exception("don't know how to convert constraint mode entity $entity")
    }

    fun toEntity(constraintMode: ConstraintMode): Int = when (constraintMode) {
        ConstraintMode.AND -> ConstraintEntity.MODE_AND
        ConstraintMode.OR -> ConstraintEntity.MODE_OR
    }
}
