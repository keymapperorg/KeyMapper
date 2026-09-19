package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.system.notifications.PostedNotification
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class NotificationConstraintMatcherTest {

    private fun notification(
        packageName: String = "com.termux",
        title: String? = "keymap_mode=1",
        text: String? = "Session running",
    ) = PostedNotification(
        key = "key",
        packageName = packageName,
        title = title,
        text = text,
    )

    private fun matches(
        data: ConstraintData.NotificationPosted,
        notification: PostedNotification = notification(),
    ) = NotificationConstraintMatcher.matches(notification, data)

    private fun title(text: String, matchMode: TextMatchMode = TextMatchMode.CONTAINS) =
        ConstraintData.NotificationPosted.Title(text, matchMode)

    @Test
    fun `contains matches a substring of the title and the text`() {
        assertThat(matches(title("mode=1")), `is`(true))
        assertThat(
            matches(ConstraintData.NotificationPosted.Text("running", TextMatchMode.CONTAINS)),
            `is`(true),
        )
    }

    @Test
    fun `contains does not match text that is not there`() {
        assertThat(matches(title("mode=2")), `is`(false))
    }

    @Test
    fun `exact requires the whole value`() {
        assertThat(matches(title("keymap_mode=1", TextMatchMode.EXACT)), `is`(true))
        assertThat(matches(title("keymap_mode", TextMatchMode.EXACT)), `is`(false))

        // The case exact matching is there for: a prefix must not match a longer mode.
        assertThat(
            matches(
                title("keymap_mode=1", TextMatchMode.EXACT),
                notification = notification(title = "keymap_mode=10"),
            ),
            `is`(false),
        )
    }

    @Test
    fun `matching always ignores case`() {
        assertThat(matches(title("KEYMAP")), `is`(true))
        assertThat(matches(title("KEYMAP_MODE=1", TextMatchMode.EXACT)), `is`(true))
    }

    @Test
    fun `the app is compared exactly`() {
        assertThat(matches(ConstraintData.NotificationPosted.FromApp("com.termux")), `is`(true))
        assertThat(matches(ConstraintData.NotificationPosted.FromApp("com.term")), `is`(false))
    }

    @Test
    fun `a null field never matches`() {
        assertThat(
            matches(title("anything"), notification = notification(title = null)),
            `is`(false),
        )
        assertThat(
            matches(
                ConstraintData.NotificationPosted.Text("anything", TextMatchMode.CONTAINS),
                notification = notification(text = null),
            ),
            `is`(false),
        )
    }
}
