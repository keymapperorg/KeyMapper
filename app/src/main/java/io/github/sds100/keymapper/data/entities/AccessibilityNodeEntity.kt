package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class AccessibilityNodeEntity(
    val id: Long = 0L,
    val parentId: Long? = null,
    val packageName: String,
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val viewResourceId: String?,
    val uniqueId: String?,
    /**
     * A list of the allowed accessibility node actions.
     */
    val actions: List<Int>,
    /**
     * The accessibility action id of how the user interacted
     * with this node. This is null if the user didn't interact with
     * this node.
     */
    val userInteractedActionId: Int?,
) : Parcelable
