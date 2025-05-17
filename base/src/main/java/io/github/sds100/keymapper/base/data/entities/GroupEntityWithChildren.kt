package io.github.sds100.keymapper.base.data.entities

import androidx.room.Embedded
import androidx.room.Relation
import io.github.sds100.keymapper.data.db.dao.GroupDao

data class GroupEntityWithChildren(
    @Embedded
    val group: GroupEntity,

    @Relation(
        parentColumn = GroupDao.KEY_UID,
        entityColumn = GroupDao.KEY_PARENT_UID,
    )
    val children: List<GroupEntity>,
)
