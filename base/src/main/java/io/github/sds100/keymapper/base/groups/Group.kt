package io.github.sds100.keymapper.base.groups

import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.constraints.ConstraintStateEntityMapper
import io.github.sds100.keymapper.data.entities.GroupEntity

data class Group(
    val uid: String,
    val name: String,
    val constraintState: ConstraintState,
    val parentUid: String?,
    val lastOpenedDate: Long,
)

object GroupEntityMapper {
    fun fromEntity(entity: GroupEntity): Group {
        val constraintState = ConstraintStateEntityMapper.fromEntity(
            entity.constraintList,
            entity.constraintMode,
        )

        return Group(
            uid = entity.uid,
            name = entity.name,
            constraintState = constraintState,
            parentUid = entity.parentUid,
            lastOpenedDate = entity.lastOpenedDate ?: System.currentTimeMillis(),
        )
    }

    fun toEntity(group: Group): GroupEntity {
        val (constraintList, constraintMode) =
            ConstraintStateEntityMapper.toEntity(group.constraintState)

        return GroupEntity(
            uid = group.uid,
            name = group.name,
            constraintList = constraintList,
            constraintMode = constraintMode,
            parentUid = group.parentUid,
            lastOpenedDate = group.lastOpenedDate,
        )
    }
}
