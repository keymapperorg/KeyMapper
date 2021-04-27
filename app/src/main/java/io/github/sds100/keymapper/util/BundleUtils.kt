package io.github.sds100.keymapper.ui.utils

import android.os.Bundle
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by sds100 on 15/03/2021.
 */

inline fun <reified T> Bundle.getJsonSerializable(key: String): T? {
    return getString(key)?.let { Json.decodeFromString<T>(it) }
}

inline fun <reified T> Bundle.putJsonSerializable(key: String, value: T) {
    return putString(key, Json.encodeToString(value))
}