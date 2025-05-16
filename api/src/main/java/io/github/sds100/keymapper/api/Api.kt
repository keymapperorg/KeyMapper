package io.github.sds100.keymapper.api

/**
 * Created by sds100 on 17/06/2021.
 */
object Api {
    // Do not use the package name for debug/ci builds
    const val ACTION_TRIGGER_KEYMAP_BY_UID =
        "io.github.sds100.keymapper.ACTION_TRIGGER_KEYMAP_BY_UID"
    const val EXTRA_KEYMAP_UID = "io.github.sds100.keymapper.EXTRA_KEYMAP_UID"

    const val ACTION_PAUSE_MAPPINGS = "io.github.sds100.keymapper.ACTION_PAUSE_MAPPINGS"
    const val ACTION_RESUME_MAPPINGS = "io.github.sds100.keymapper.ACTION_RESUME_MAPPINGS"
    const val ACTION_TOGGLE_MAPPINGS = "io.github.sds100.keymapper.ACTION_TOGGLE_MAPPINGS"
}
