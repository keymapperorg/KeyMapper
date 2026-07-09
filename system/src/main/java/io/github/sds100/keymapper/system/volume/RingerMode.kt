package io.github.sds100.keymapper.system.volume

import kotlinx.serialization.Serializable

@Serializable
enum class RingerMode {
    NORMAL,
    VIBRATE,
    SILENT,
}
