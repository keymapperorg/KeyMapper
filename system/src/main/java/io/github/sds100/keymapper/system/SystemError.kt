package io.github.sds100.keymapper.system

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.common.result.Error
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.ui.ResourceProvider

sealed class SystemError : Error() {
    data class PermissionDenied(val permission: Permission) : Error() {
        companion object {

            fun getMessageForPermission(
                resourceProvider: ResourceProvider,
                permission: Permission,
            ): String {
                val resId = when (permission) {
                    Permission.WRITE_SETTINGS -> R.string.error_action_requires_write_settings_permission
                    Permission.CAMERA -> R.string.error_action_requires_camera_permission
                    Permission.DEVICE_ADMIN -> R.string.error_need_to_enable_device_admin
                    Permission.READ_PHONE_STATE -> R.string.error_action_requires_read_phone_state_permission
                    Permission.ACCESS_NOTIFICATION_POLICY -> R.string.error_action_notification_policy_permission
                    Permission.WRITE_SECURE_SETTINGS -> R.string.error_need_write_secure_settings_permission
                    Permission.NOTIFICATION_LISTENER -> R.string.error_denied_notification_listener_service_permission
                    Permission.CALL_PHONE -> R.string.error_denied_call_phone_permission
                    Permission.ROOT -> R.string.error_requires_root
                    Permission.IGNORE_BATTERY_OPTIMISATION -> R.string.error_battery_optimisation_enabled
                    Permission.SHIZUKU -> R.string.error_shizuku_permission_denied
                    Permission.ACCESS_FINE_LOCATION -> R.string.error_access_fine_location_permission_denied
                    Permission.ANSWER_PHONE_CALL -> R.string.error_answer_end_phone_call_permission_denied
                    Permission.FIND_NEARBY_DEVICES -> R.string.error_find_nearby_devices_permission_denied
                    Permission.POST_NOTIFICATIONS -> R.string.error_notifications_permission_denied
                }

                return resourceProvider.getString(resId)
            }
        }
    }

    data class ImeDisabled(val ime: ImeInfo) : Error()
}
