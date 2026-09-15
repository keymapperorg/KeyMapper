package io.github.sds100.keymapper.base.onboarding

data class WhatsNewState(val versionName: String, val notes: WhatsNewNotes)

data class WhatsNewNotes(
    val new: List<WhatsNewItem>,
    val improved: List<WhatsNewItem>,
    val fixed: List<WhatsNewItem>,
)

data class WhatsNewItem(
    val title: String,
    val description: String?,
    val isSpotlight: Boolean = false,
)

/**
 * Parses the in-app release notes in whats-new-app.txt. Each line starts with a prefix:
 * "!" spotlight, "+" new, "*" improved, "-" fixed. Text after the first "|" is a description.
 * Lines with an unknown prefix are treated as improvements.
 */
object WhatsNewParser {
    fun parse(text: String): WhatsNewNotes {
        val new = mutableListOf<WhatsNewItem>()
        val improved = mutableListOf<WhatsNewItem>()
        val fixed = mutableListOf<WhatsNewItem>()

        for (rawLine in text.lines()) {
            val line = rawLine.trim()

            if (line.isEmpty()) {
                continue
            }

            val prefix = line.first()
            val hasKnownPrefix = prefix in setOf('!', '+', '*', '-')
            val content = if (hasKnownPrefix) line.drop(1) else line.trimStart('•')
            val item = parseItem(content) ?: continue

            when (prefix) {
                '!' -> new.add(item.copy(isSpotlight = true))
                '+' -> new.add(item)
                '-' -> fixed.add(item)
                else -> improved.add(item)
            }
        }

        return WhatsNewNotes(new, improved, fixed)
    }

    private fun parseItem(content: String): WhatsNewItem? {
        val title = content.substringBefore('|').trim()

        if (title.isEmpty()) {
            return null
        }

        val description = if (content.contains('|')) {
            content.substringAfter('|').trim().ifEmpty { null }
        } else {
            null
        }

        return WhatsNewItem(title, description)
    }
}
