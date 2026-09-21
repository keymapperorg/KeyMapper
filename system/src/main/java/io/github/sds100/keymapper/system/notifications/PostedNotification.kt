package io.github.sds100.keymapper.system.notifications

/**
 * A notification posted by another app.
 */
data class PostedNotification(
    /**
     * The key the system uses to identify this notification. Apps reuse it when they update a
     * notification in place.
     */
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
)
