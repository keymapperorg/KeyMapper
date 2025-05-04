package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.sds100.keymapper.actions.uielement.NodeInteractionType
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao.Companion.KEY_ACTIONS
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao.Companion.KEY_CLASS_NAME
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao.Companion.KEY_CONTENT_DESCRIPTION
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao.Companion.KEY_ID
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao.Companion.KEY_PACKAGE_NAME
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao.Companion.KEY_TEXT
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao.Companion.KEY_UNIQUE_ID
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao.Companion.KEY_VIEW_RESOURCE_ID
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao.Companion.TABLE_NAME
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Entity(tableName = TABLE_NAME,)
data class AccessibilityNodeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = KEY_ID)
    val id: Long = 0L,

    @ColumnInfo(name = KEY_PACKAGE_NAME)
    val packageName: String,

    @ColumnInfo(name = KEY_TEXT)
    val text: String?,

    @ColumnInfo(name = KEY_CONTENT_DESCRIPTION)
    val contentDescription: String?,

    @ColumnInfo(name = KEY_CLASS_NAME)
    val className: String?,

    @ColumnInfo(name = KEY_VIEW_RESOURCE_ID)
    val viewResourceId: String?,

    @ColumnInfo(name = KEY_UNIQUE_ID)
    val uniqueId: String?,

    @ColumnInfo(name = KEY_ACTIONS)
    val actions: Set<NodeInteractionType>,
) : Parcelable
