package io.github.sds100.keymapper.base.onboarding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhatsNewParserTest {

    @Test
    fun `parse each prefix into its section`() {
        val notes = WhatsNewParser.parse(
            """
            ! Spotlight
            + New feature
            * Improvement
            - Bug fix
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                WhatsNewItem("Spotlight", null, isSpotlight = true),
                WhatsNewItem("New feature", null),
            ),
            notes.new,
        )
        assertEquals(listOf(WhatsNewItem("Improvement", null)), notes.improved)
        assertEquals(listOf(WhatsNewItem("Bug fix", null)), notes.fixed)
    }

    @Test
    fun `parse description after pipe`() {
        val notes = WhatsNewParser.parse("! Title | Some description")

        assertEquals(
            listOf(WhatsNewItem("Title", "Some description", isSpotlight = true)),
            notes.new,
        )
    }

    @Test
    fun `empty description is null`() {
        val notes = WhatsNewParser.parse("+ Title |  ")

        assertEquals(listOf(WhatsNewItem("Title", null)), notes.new)
    }

    @Test
    fun `skip blank lines and lines without a title`() {
        val notes = WhatsNewParser.parse("\n   \n+ Feature\n+\n\n")

        assertEquals(listOf(WhatsNewItem("Feature", null)), notes.new)
    }

    @Test
    fun `unknown prefix is treated as an improvement`() {
        val notes = WhatsNewParser.parse("• Old style bullet\nNo prefix")

        assertEquals(
            listOf(WhatsNewItem("Old style bullet", null), WhatsNewItem("No prefix", null)),
            notes.improved,
        )
    }

    @Test
    fun `each spotlight line becomes a spotlight item in new`() {
        val notes = WhatsNewParser.parse("! First\n! Second")

        assertEquals(
            listOf(
                WhatsNewItem("First", null, isSpotlight = true),
                WhatsNewItem("Second", null, isSpotlight = true),
            ),
            notes.new,
        )
    }

    @Test
    fun `parse the real whats-new-app asset file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val text = context.assets.open("whats-new-app.txt")
            .bufferedReader()
            .use { it.readText() }

        // The release notes change every release, so just confirm the real asset file parses
        // without throwing rather than pinning exact content that would need updating each time.
        WhatsNewParser.parse(text)
    }
}
