package io.github.sds100.keymapper.base.system.notifications

import android.os.Build
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.notifications.NotificationAdapter
import io.github.sds100.keymapper.system.notifications.NotificationChannelModel
import io.github.sds100.keymapper.system.notifications.NotificationModel
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ManageNotificationsUseCaseImpl @Inject constructor(
    private val preferences: PreferenceRepository,
    private val notificationAdapter: NotificationAdapter,
    private val suAdapter: SuAdapter,
    private val permissionAdapter: PermissionAdapter,
) : ManageNotificationsUseCase {

    override val showImePickerNotification: Flow<Boolean> =
        combine(
            suAdapter.isGranted,
            preferences.get(Keys.showImePickerNotification),
        ) { hasRootPermission, show ->
            when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> show ?: false

                /*
                always show the notification on Oreo+ because the system/user controls
                whether notifications are shown.
                 */
                Build.VERSION.SDK_INT == Build.VERSION_CODES.O -> true

                /*
                This needs root permission on API 27 and 28
                 */
                (
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1 ||
                        Build.VERSION.SDK_INT == Build.VERSION_CODES.P
                    ) &&
                    hasRootPermission -> true

                else -> false
            }
        }

    override val showToggleKeyboardNotification: Flow<Boolean> =
        preferences.get(Keys.showToggleKeyboardNotification).map {
            // always show the notification on Oreo+ because the system/user controls whether notifications are shown
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                true
            } else {
                it ?: true
            }
        }

    override val showToggleMappingsNotification: Flow<Boolean> =
        preferences.get(Keys.showToggleKeyMapsNotification).map {
            // always show the notification on Oreo+ because the system/user controls whether notifications are shown
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                true
            } else {
                it ?: true
            }
        }

    override val onActionClick: Flow<String> = notificationAdapter.onNotificationActionClick

    override fun show(notification: NotificationModel) {
        notificationAdapter.showNotification(notification)
    }

    override fun dismiss(notificationId: Int) {
        notificationAdapter.dismissNotification(notificationId)
    }

    override fun createChannel(channel: NotificationChannelModel) {
        notificationAdapter.createChannel(channel)
    }

    override fun deleteChannel(channelId: String) {
        notificationAdapter.deleteChannel(channelId)
    }

    override fun isPermissionGranted(): Boolean {
        return permissionAdapter.isGranted(Permission.POST_NOTIFICATIONS)
    }
}

interface ManageNotificationsUseCase {
    val showImePickerNotification: Flow<Boolean>
    val showToggleKeyboardNotification: Flow<Boolean>
    val showToggleMappingsNotification: Flow<Boolean>

    /**
     * The string is the ID of the action.
     */
    val onActionClick: Flow<String>

    fun isPermissionGranted(): Boolean
    fun show(notification: NotificationModel)
    fun dismiss(notificationId: Int)
    fun createChannel(channel: NotificationChannelModel)
    fun deleteChannel(channelId: String)
}
