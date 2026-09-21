package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.system.notifications.PostedNotification

/**
 * Matches a notification against a [ConstraintData.NotificationPosted].
 */
object NotificationConstraintMatcher {

    fun matches(
        notification: PostedNotification,
        data: ConstraintData.NotificationPosted,
    ): Boolean = when (data) {
        is ConstraintData.NotificationPosted.FromApp ->
            notification.packageName == data.packageName

        is ConstraintData.NotificationPosted.Title -> matchesText(notification.title, data)

        is ConstraintData.NotificationPosted.Text -> matchesText(notification.text, data)
    }

    private fun matchesText(
        fieldValue: String?,
        data: ConstraintData.NotificationPosted.TextMatch,
    ): Boolean {
        fieldValue ?: return false

        return when (data.matchMode) {
            TextMatchMode.CONTAINS -> fieldValue.contains(data.text, ignoreCase = true)
            TextMatchMode.EXACT -> fieldValue.equals(data.text, ignoreCase = true)
        }
    }
}
