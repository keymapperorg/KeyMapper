package io.github.sds100.keymapper.data.entities

import androidx.room.Embedded
import androidx.room.Relation
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao
import io.github.sds100.keymapper.data.db.dao.FloatingLayoutDao

data class FloatingLayoutEntityWithButtons(
    @Embedded
    val layout: FloatingLayoutEntity,

    @Relation(
        parentColumn = FloatingLayoutDao.KEY_UID,
        entityColumn = FloatingButtonDao.KEY_LAYOUT_UID,
    )
    val buttons: List<FloatingButtonEntity>,
)
