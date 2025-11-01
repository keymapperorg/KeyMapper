package io.github.sds100.keymapper.common.utils

import android.os.Bundle
import kotlinx.serialization.json.Json

inline fun <reified T> Bundle.getJsonSerializable(key: String): T? = getString(key)?.let {
    Json.decodeFromString<T>(it)
}

inline fun <reified T> Bundle.putJsonSerializable(key: String, value: T) =
    putString(key, Json.encodeToString(value))
