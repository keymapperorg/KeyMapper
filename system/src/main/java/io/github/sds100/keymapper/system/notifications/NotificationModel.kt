package io.github.sds100.keymapper.system.notifications

import androidx.annotation.DrawableRes


data class NotificationModel(
    val id: Int,
    val channel: String,
    val title: String,
    val text: String,
    @DrawableRes val icon: Int,
    /**
     * Null if nothing should happen when the notification is tapped.
     */
    val onClickAction: NotificationIntentType? = null,
    val showOnLockscreen: Boolean,
    val onGoing: Boolean,
    val priority: Int,
    val actions: List<Action> = emptyList(),
    val autoCancel: Boolean = false,
    val bigTextStyle: Boolean = false,
) {
    data class Action(val text: String, val intentType: NotificationIntentType)
}

/**
 * Due to restrictions on notification trampolines in Android 12+ you can't launch
 * activities from a broadcast receiver in response to a notification action.
 */
sealed class NotificationIntentType {
    /**
     * Broadcast an intent to the NotificationReceiver.
     */
    data class Broadcast(val action: String) : NotificationIntentType()

    /**
     * Launch the main activity with the specified action in the intent. If it is null
     * then it will just launch the activity without a custom action.
     */
    data class MainActivity(val customIntentAction: String? = null) : NotificationIntentType()

    data class Activity(val action: String) : NotificationIntentType()
}
