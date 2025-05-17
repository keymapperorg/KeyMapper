package io.github.sds100.keymapper.base.groups

import io.github.sds100.keymapper.constraints.ConstraintEntityMapper
import io.github.sds100.keymapper.constraints.ConstraintModeEntityMapper
import io.github.sds100.keymapper.constraints.ConstraintState
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
        val constraintList =
            entity.constraintList.map { ConstraintEntityMapper.fromEntity(it) }.toSet()

        val constraintMode = ConstraintModeEntityMapper.fromEntity(entity.constraintMode)

        return Group(
            uid = entity.uid,
            name = entity.name,
            constraintState = ConstraintState(constraintList, constraintMode),
            parentUid = entity.parentUid,
            lastOpenedDate = entity.lastOpenedDate ?: System.currentTimeMillis(),
        )
    }

    fun toEntity(group: Group): GroupEntity {
        return GroupEntity(
            uid = group.uid,
            name = group.name,
            constraintList = group.constraintState.constraints.map {
                ConstraintEntityMapper.toEntity(it)
            },
            constraintMode = ConstraintModeEntityMapper.toEntity(group.constraintState.mode),
            parentUid = group.parentUid,
            lastOpenedDate = group.lastOpenedDate,
        )
    }
}
