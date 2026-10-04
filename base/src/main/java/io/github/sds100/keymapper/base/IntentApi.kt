package io.github.sds100.keymapper.base

object IntentApi {
    const val ACTION_TRIGGER_KEYMAP_BY_UID =
        "io.github.sds100.keymapper.ACTION_TRIGGER_KEYMAP_BY_UID"
    const val EXTRA_KEYMAP_UID = "io.github.sds100.keymapper.EXTRA_KEYMAP_UID"

    const val TRIGGER_RECEIVER_CLASS =
        "io.github.sds100.keymapper.api.TriggerKeyMapsBroadcastReceiver"

    const val ACTION_PAUSE_MAPPINGS = "io.github.sds100.keymapper.ACTION_PAUSE_MAPPINGS"
    const val ACTION_RESUME_MAPPINGS = "io.github.sds100.keymapper.ACTION_RESUME_MAPPINGS"
    const val ACTION_TOGGLE_MAPPINGS = "io.github.sds100.keymapper.ACTION_TOGGLE_MAPPINGS"

    const val PAUSE_RECEIVER_CLASS =
        "io.github.sds100.keymapper.api.PauseMappingsBroadcastReceiver"

    const val ACTION_ENABLE_GROUP = "io.github.sds100.keymapper.ACTION_ENABLE_GROUP"
    const val ACTION_DISABLE_GROUP = "io.github.sds100.keymapper.ACTION_DISABLE_GROUP"
    const val ACTION_TOGGLE_GROUP = "io.github.sds100.keymapper.ACTION_TOGGLE_GROUP"
    const val EXTRA_GROUP_UID = "io.github.sds100.keymapper.EXTRA_GROUP_UID"

    const val GROUP_RECEIVER_CLASS =
        "io.github.sds100.keymapper.api.EnableGroupBroadcastReceiver"
}
