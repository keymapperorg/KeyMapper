package io.github.sds100.keymapper.util

/**
 * Created by sds100 on 19/03/2021.
 */

/**
 * Not for high speed stuff.
 */
fun <K, V> Map<K, V>.getKey(value: V) =
    entries.firstOrNull { it.value == value }?.key
