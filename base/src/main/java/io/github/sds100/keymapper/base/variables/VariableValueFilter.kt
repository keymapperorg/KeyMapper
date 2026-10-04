package io.github.sds100.keymapper.base.variables

/**
 * Only a minus sign at the start and digits make sense, and filtering as it is typed avoids
 * having to show an error for anything else.
 */
fun String.filterVariableValue(): String {
    val digits = filter(Char::isDigit)

    return if (startsWith('-')) {
        "-$digits"
    } else {
        digits
    }
}
