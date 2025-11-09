package io.github.sds100.keymapper.system.settings

import kotlinx.serialization.Serializable

@Serializable
enum class SettingType {
    SYSTEM,
    SECURE,
    GLOBAL,
}
