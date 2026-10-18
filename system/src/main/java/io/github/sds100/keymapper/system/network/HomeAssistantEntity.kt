package io.github.sds100.keymapper.system.network

data class HomeAssistantEntity(
    val entityId: String,
    val friendlyName: String,
    val domain: String,
    val state: String,
)
