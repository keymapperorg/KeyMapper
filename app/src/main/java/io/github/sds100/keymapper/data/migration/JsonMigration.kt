package io.github.sds100.keymapper.data.migration

import com.google.gson.JsonObject

class JsonMigration(
    val versionBefore: Int,
    val versionAfter: Int,
    val migrate: (json: JsonObject) -> JsonObject,
)
