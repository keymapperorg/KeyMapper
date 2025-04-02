package io.github.sds100.keymapper.data.entities

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
)
