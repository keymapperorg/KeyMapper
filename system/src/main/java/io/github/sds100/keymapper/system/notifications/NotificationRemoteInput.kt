package io.github.sds100.keymapper.system.notifications

import io.github.sds100.keymapper.common.notifications.KMNotificationAction

/**
 * Represents text input from a notification action with RemoteInput.
 * @param intentAction The intent action that triggered the text input
 * @param text The text that was inputted by the user
 */
data class NotificationRemoteInput(
    val intentAction: KMNotificationAction.IntentAction,
    val text: String
)