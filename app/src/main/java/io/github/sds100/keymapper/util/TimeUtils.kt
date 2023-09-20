package io.github.sds100.keymapper.util

fun formatSeconds(seconds: Int, divider: String = ":"): String {
    val min = (seconds / 60).toString().padStart(2, '0').padEnd(2, '0')
    val sec = (seconds % 60).toString().padStart(2, '0').padEnd(2, '0')
    return "$min$divider$sec"
}