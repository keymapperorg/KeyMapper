package io.github.sds100.keymapper.base.bugreport

/**
 * Parses stdout from a `find` command listing absolute paths to keylayout files.
 */
internal fun parseKeylayoutPaths(output: String): List<String> =
    output.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.startsWith('/') }
        .distinct()
        .toList()
