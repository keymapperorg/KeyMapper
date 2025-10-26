package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import io.github.sds100.keymapper.data.db.dao.GroupDao
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import kotlinx.parcelize.Parcelize

@Parcelize
data class KeyMapEntitiesWithGroup(
    @Embedded
    val group: GroupEntity,
    @Relation(
        parentColumn = GroupDao.KEY_UID,
        entityColumn = KeyMapDao.KEY_GROUP_UID,
    )
    val keyMaps: List<KeyMapEntity>,
) : Parcelable
