package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao
import io.github.sds100.keymapper.data.db.dao.FloatingLayoutDao
import kotlinx.parcelize.Parcelize

@Parcelize
data class FloatingButtonEntityWithLayout(
    @Embedded val button: FloatingButtonEntity,

    @Relation(
        parentColumn = FloatingButtonDao.KEY_LAYOUT_UID,
        entityColumn = FloatingLayoutDao.KEY_UID,
    )
    val layout: FloatingLayoutEntity,
) : Parcelable
