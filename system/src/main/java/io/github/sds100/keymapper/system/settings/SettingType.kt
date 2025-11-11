package io.github.sds100.keymapper.system.settings

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
@Keep
enum class SettingType {
    SYSTEM,
    SECURE,
    GLOBAL,
}
