package io.github.sds100.keymapper.system.permissions

enum class Permission {
    WRITE_SETTINGS,
    CAMERA,
    DEVICE_ADMIN,
    READ_PHONE_STATE,
    ACCESS_NOTIFICATION_POLICY,
    WRITE_SECURE_SETTINGS,
    NOTIFICATION_LISTENER,
    CALL_PHONE,
    SEND_SMS,
    ROOT,
    IGNORE_BATTERY_OPTIMISATION,
    SHIZUKU,
    ACCESS_FINE_LOCATION,
    ANSWER_PHONE_CALL,
    FIND_NEARBY_DEVICES,
    POST_NOTIFICATIONS,
    READ_LOGS,
    ACCESS_LOCAL_NETWORK,

    /**
     * Gives access to read all external files. Only requested
     * on FOSS build because Google Play usually only permit it for
     * file manager applications. This is needed on Android TV because
     * it stubs the APIs for creating/reading documents.
     */
    MANAGE_EXTERNAL_STORAGE,
}
