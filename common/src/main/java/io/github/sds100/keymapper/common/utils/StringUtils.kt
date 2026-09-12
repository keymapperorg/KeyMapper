package io.github.sds100.keymapper.common.utils

import kotlin.math.roundToInt

fun String.getWordBoundaries(cursorPosition: Int): Pair<Int, Int>? {
    if (this.isBlank()) return null

    // return null if there is just whitespace around the position

    if (getOrNull(cursorPosition - 1)?.isWhitespace() == true &&
        getOrNull(cursorPosition)?.isWhitespace() == true
    ) {
        return null
    }

    var lastSpaceIndex: Int? = null
    var firstBoundary: Int? = null
    var secondBoundary: Int? = null

    for ((index, c) in this.withIndex()) {
        if (c.isWhitespace()) {
            lastSpaceIndex = index

            if (index > cursorPosition) {
                secondBoundary = lastSpaceIndex
                break
            }
        }

        /*
        If the cursor is at the end of the line then it is outside the character index range so check for this case
        check if we are at the end of the line.
         */
        if (cursorPosition == this.length &&
            index == this.lastIndex ||
            index == cursorPosition
        ) {
            firstBoundary = lastSpaceIndex?.plus(1)
        }
    }

    return Pair(firstBoundary ?: 0, secondBoundary ?: lastIndex)
}

fun Float.toPercentString(): String {
    return "${(this * 100).roundToInt()}%"
}

/**
 * Replace Windows (\r\n) and classic Mac (\r) line endings with Unix (\n) ones. A shell does not
 * treat \r as whitespace so a trailing \r stops reserved words like "then" and "else" from being
 * recognized, which breaks multi-line scripts pasted from a computer. See issue #2209.
 */
fun String.normalizeLineEndings(): String {
    return replace("\r\n", "\n").replace('\r', '\n')
}
