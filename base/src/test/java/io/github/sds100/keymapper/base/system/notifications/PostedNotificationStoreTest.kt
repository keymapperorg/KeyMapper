package io.github.sds100.keymapper.base.system.notifications

import io.github.sds100.keymapper.system.notifications.PostedNotification
import io.github.sds100.keymapper.system.notifications.PostedNotificationStore
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.`is`
import org.junit.Test

class PostedNotificationStoreTest {

    private val store = PostedNotificationStore()

    private fun notification(key: String = "key", title: String? = "keymap_mode=1") =
        PostedNotification(
            key = key,
            packageName = "com.termux",
            title = title,
            text = "Session running",
        )

    @Test
    fun `posting adds to the active list`() {
        store.onPosted(notification())

        assertThat(store.active.value.size, `is`(1))
    }

    @Test
    fun `the active list is in the order the notifications were posted`() {
        // Dismissing the most recent notification relies on this order.
        store.onPosted(notification(key = "first", title = "first"))
        store.onPosted(notification(key = "second", title = "second"))
        store.onPosted(notification(key = "third", title = "third"))

        assertThat(store.active.value.map { it.title }, contains("first", "second", "third"))
        assertThat(store.active.value.last().key, `is`("third"))
    }

    @Test
    fun `updating a notification in place moves it to the end`() {
        store.onPosted(notification(key = "first", title = "first"))
        store.onPosted(notification(key = "second", title = "second"))
        store.onPosted(notification(key = "first", title = "first updated"))

        assertThat(store.active.value.map { it.title }, contains("second", "first updated"))
    }

    @Test
    fun `removing a notification takes it out of the active list`() {
        store.onPosted(notification(key = "first"))
        store.onPosted(notification(key = "second"))

        store.onRemoved("second")

        assertThat(store.active.value.single().key, `is`("first"))
    }

    @Test
    fun `seeding on reconnect replaces whatever was there`() {
        store.onPosted(notification(key = "stale"))

        store.seed(listOf(notification(key = "fresh")))

        assertThat(store.active.value.single().key, `is`("fresh"))
    }

    @Test
    fun `clearing removes everything so no content is left in memory`() {
        store.onPosted(notification())

        store.clear()

        assertThat(store.active.value, `is`(empty()))
    }
}
