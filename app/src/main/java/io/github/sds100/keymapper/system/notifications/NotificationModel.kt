package io.github.sds100.keymapper.system.notifications

import androidx.annotation.DrawableRes

/**
 * Created by sds100 on 16/04/2021.
 */
data class NotificationModel(
    val id: Int,
    val channel: String,
    val title: String,
    val text: String,
    @DrawableRes val icon: Int,
    /**
     * The id to send back to the notification id when the notification is tapped or null if nothing
     * should happen when the notification is tapped.
     */
    val onClickActionId: String?,
    val showOnLockscreen: Boolean,
    val onGoing: Boolean,
    val priority: Int,
    val actions: List<Action> = emptyList(),
    val autoCancel: Boolean = false,
    val bigTextStyle: Boolean = false,
) {

    data class Action(val id: String, val text: String)
}
